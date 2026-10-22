package com.vote.controller;

import com.vote.admin.AdminAuthService;
import com.vote.data.constants.Constants;
import com.vote.data.po.CommentInfo;
import com.vote.data.po.SnapInfo;
import com.vote.data.po.User;
import com.vote.data.vo.ResponseVO;
import com.vote.exception.BusinessException;
import com.vote.mappers.CommentInfoMapper;
import com.vote.mappers.SnapInfoMapper;
import com.vote.mappers.UserMapper;
import com.vote.query.CommentInfoQuery;
import com.vote.query.SimplePage;
import com.vote.query.SnapInfoQuery;
import com.vote.server.impl.SnapScoreService;
import com.vote.util.SnowflakeIdGenerator;
import org.springframework.util.StringUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** JSON API used by the standalone administration console. */
@RestController
@RequestMapping("/admin/api")
public class AdminController extends ABaseController {
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final SecureRandom RANDOM = new SecureRandom();

    @Resource private AdminAuthService adminAuthService;
    @Resource private UserMapper userMapper;
    @Resource private SnapInfoMapper<SnapInfo, SnapInfoQuery> snapMapper;
    @Resource private CommentInfoMapper<CommentInfo, CommentInfoQuery> commentMapper;
    @Resource private SnowflakeIdGenerator idGenerator;
    @Resource private SnapScoreService snapScoreService;

    @GetMapping("/session")
    public ResponseVO session(HttpServletRequest request) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("authenticated", adminAuthService.isAuthenticated(request));
        result.put("keyConfigured", adminAuthService.isKeyConfigured());
        return getSuccessResponseVO(result);
    }

    @PostMapping("/auth")
    public ResponseVO authenticate(@RequestParam(required = false) String apiKey, HttpServletRequest request) {
        if (!adminAuthService.isValidKey(apiKey) && !adminAuthService.isAuthenticated(request)) {
            throw new BusinessException(403, "管理员密钥无效");
        }
        return getSuccessResponseVO(true);
    }

    @GetMapping("/overview")
    public ResponseVO overview(HttpServletRequest request) {
        require(request);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("users", userMapper.adminSelectCount(null, null));
        data.put("activeUsers", userMapper.adminCountByStatus(0));
        data.put("snaps", snapMapper.adminSelectCount(null, null, null));
        data.put("visibleSnaps", snapMapper.adminCountByStatus(0));
        data.put("comments", commentMapper.adminCountByStatus(null));
        data.put("pendingSnaps", snapMapper.adminCountByStatus(2));
        return getSuccessResponseVO(data);
    }

    @GetMapping("/users")
    public ResponseVO users(@RequestParam(defaultValue = "1") Integer page,
                            @RequestParam(defaultValue = "20") Integer size,
                            String keyword, Integer status, HttpServletRequest request) {
        require(request);
        Page pageInfo = page(page, size);
        String query = clean(keyword);
        Integer state = normalizeStatus(status);
        List<User> items = userMapper.adminSelectList(query, state, pageInfo.size, pageInfo.offset);
        List<Map<String, Object>> result = new ArrayList<>();
        for (User user : items) result.add(userView(user));
        return getSuccessResponseVO(pageResult(result, userMapper.adminSelectCount(query, state), pageInfo));
    }

    @PostMapping("/users/save")
    public ResponseVO saveUser(@RequestParam(required = false) Long userId,
                               @RequestParam String loginName,
                               @RequestParam(required = false) String password,
                               @RequestParam String name,
                               String description, Integer sex, String major, Integer status,
                               HttpServletRequest request) {
        require(request);
        String login = required(loginName, "账号不能为空");
        String displayName = required(name, "昵称不能为空");
        if (displayName.length() > 64 || login.length() > 64) throw new BusinessException("账号或昵称过长");
        if (description != null && description.length() > 500) throw new BusinessException("简介不能超过 500 字");
        validateSex(sex);
        Integer effectiveSex = sex != null && sex == -1 ? null : sex;
        Integer state = normalizeStatus(status);
        int nextStatus = state == null ? 0 : state;
        if (userId == null) {
            if (!StringUtils.hasText(password) || password.length() < 6) throw new BusinessException("新用户密码至少 6 位");
            if (userMapper.selectByLoginName(login) != null) throw new BusinessException("账号已存在");
            User user = User.builder().userId(idGenerator.nextId()).loginName(login)
                    .passwordHash(hashPassword(password)).name(displayName).description(description)
                    .sex(effectiveSex).major(major).status(nextStatus)
                    .ctime((int) (System.currentTimeMillis() / 1000)).build();
            userMapper.insert(user);
            return getSuccessResponseVO(userView(userMapper.selectByUserId(user.getUserId())));
        }
        User existing = userMapper.selectByUserId(userId);
        if (existing == null) throw new BusinessException("用户不存在");
        User sameLogin = userMapper.selectByLoginName(login);
        if (sameLogin != null && !userId.equals(sameLogin.getUserId())) throw new BusinessException("账号已存在");
        existing.setLoginName(login); existing.setName(displayName); existing.setDescription(description);
        // Keep -1 as an explicit clear instruction for the dynamic mapper.
        existing.setSex(sex == null ? existing.getSex() : sex);
        existing.setMajor(major); existing.setStatus(nextStatus);
        if (StringUtils.hasText(password)) {
            if (password.length() < 6) throw new BusinessException("密码至少 6 位");
            existing.setPasswordHash(hashPassword(password));
        }
        userMapper.adminUpdate(existing);
        return getSuccessResponseVO(userView(userMapper.selectByUserId(userId)));
    }

    @DeleteMapping("/users")
    public ResponseVO deleteUser(@RequestParam Long userId, HttpServletRequest request) {
        require(request);
        if (userMapper.selectByUserId(userId) == null) throw new BusinessException("用户不存在");
        userMapper.adminUpdate(User.builder().userId(userId).status(2).build());
        return getSuccessResponseVO(true);
    }

    @GetMapping("/snaps")
    public ResponseVO snaps(@RequestParam(defaultValue = "1") Integer page,
                            @RequestParam(defaultValue = "20") Integer size,
                            String keyword, Integer type, Integer status, HttpServletRequest request) {
        require(request);
        Page pageInfo = page(page, size);
        String query = clean(keyword);
        Integer snapType = normalizeType(type);
        Integer snapStatus = normalizeSnapStatus(status);
        List<SnapInfo> items = snapMapper.adminSelectList(query, snapType, snapStatus, pageInfo.size, pageInfo.offset);
        return getSuccessResponseVO(pageResult(items, snapMapper.adminSelectCount(query, snapType, snapStatus), pageInfo));
    }

    @PostMapping("/snaps/save")
    @Transactional(rollbackFor = Exception.class)
    public ResponseVO saveSnap(@RequestParam(required = false) Integer snapId,
                               @RequestParam Long userId,
                               @RequestParam String title,
                               @RequestParam String desc,
                               @RequestParam Integer type,
                               String snapImage, Integer status, Integer score, HttpServletRequest request) {
        require(request);
        User owner = userMapper.selectByUserId(userId);
        if (owner == null) throw new BusinessException("发布用户不存在");
        String titleText = required(title, "笔记标题不能为空");
        if (titleText.length() > 100) throw new BusinessException("笔记标题不能超过 100 字");
        String content = required(desc, "笔记内容不能为空");
        if (content.length() > 500) throw new BusinessException("笔记内容不能超过 500 字");
        if (type == null || type < 0 || type > 4) throw new BusinessException("笔记类型无效");
        Integer nextStatus = normalizeSnapStatus(status);
        if (nextStatus == null) nextStatus = 0;
        SnapInfo snap;
        if (snapId == null) {
            Integer initialScore = score == null ? Constants.DefaultRankScore : score;
            snap = SnapInfo.builder().userId(userId).snapImage(snapImage == null ? null : snapImage.trim()).title(titleText).desc(content)
                    .type(type).status(nextStatus).score(initialScore).ctime(System.currentTimeMillis()).build();
            snapMapper.insert(snap);
        } else {
            snap = snapMapper.adminSelectBySnapId(snapId);
            if (snap == null) throw new BusinessException("笔记不存在");
            snap.setUserId(userId); snap.setSnapImage(snapImage == null ? null : snapImage.trim()); snap.setTitle(titleText); snap.setDesc(content);
            snap.setType(type); snap.setStatus(nextStatus);
            if (score != null) snap.setScore(score);
            snapMapper.adminUpdate(snap);
        }
        snapScoreService.syncRankAfterCommit(snap.getSnapId(), snap.getScore(), snap.getStatus());
        return getSuccessResponseVO(snapMapper.adminSelectBySnapId(snap.getSnapId()));
    }

    @DeleteMapping("/snaps")
    @Transactional(rollbackFor = Exception.class)
    public ResponseVO deleteSnap(@RequestParam Integer snapId, HttpServletRequest request) {
        require(request);
        SnapInfo snap = snapMapper.adminSelectBySnapId(snapId);
        if (snap == null) throw new BusinessException("笔记不存在");
        snapMapper.adminUpdate(SnapInfo.builder().snapId(snapId).status(1).build());
        snapScoreService.syncRankAfterCommit(snapId, snap.getScore(), 1);
        return getSuccessResponseVO(true);
    }

    @GetMapping("/comments")
    public ResponseVO comments(@RequestParam(defaultValue = "1") Integer page,
                               @RequestParam(defaultValue = "20") Integer size,
                               String keyword, Integer snapId, Integer status, HttpServletRequest request) {
        require(request);
        Page pageInfo = page(page, size);
        CommentInfoQuery query = new CommentInfoQuery();
        query.setTextFuzzy(clean(keyword)); query.setSnapId(snapId); query.setStatus(normalizeCommentStatus(status));
        query.setOrderBy("c.comment_id DESC"); query.setSimplePage(new SimplePage(pageInfo.offset, pageInfo.size));
        List<CommentInfo> items = commentMapper.selectList(query);
        return getSuccessResponseVO(pageResult(items, commentMapper.selectCount(query), pageInfo));
    }

    @PostMapping("/comments/save")
    @Transactional(rollbackFor = Exception.class)
    public ResponseVO saveComment(@RequestParam(required = false) Integer commentId,
                                  @RequestParam Integer snapId,
                                  @RequestParam String userId,
                                  @RequestParam String text,
                                  Integer status, HttpServletRequest request) {
        require(request);
        if (snapMapper.adminSelectBySnapId(snapId) == null) throw new BusinessException("笔记不存在");
        String body = required(text, "评论内容不能为空");
        if (body.length() > 1000) throw new BusinessException("评论内容不能超过 1000 字");
        Integer nextStatus = normalizeCommentStatus(status);
        if (nextStatus == null) nextStatus = 0;
        if (commentId == null) {
            CommentInfo comment = CommentInfo.builder().snapId(snapId).userId(required(userId, "评论用户不能为空"))
                    .text(body).status(nextStatus).ctime(new Date()).build();
            commentMapper.insert(comment);
            if (nextStatus == 0) snapScoreService.adjustScore(snapId, Constants.CommentRankScore);
            return getSuccessResponseVO(commentMapper.selectByCommentId(comment.getCommentId()));
        }
        CommentInfo existing = commentMapper.selectByCommentId(commentId);
        if (existing == null) throw new BusinessException("评论不存在");
        if (!snapId.equals(existing.getSnapId())) throw new BusinessException("评论所属笔记不能修改");
        int oldContribution = Integer.valueOf(0).equals(existing.getStatus()) ? Constants.CommentRankScore : 0;
        existing.setUserId(required(userId, "评论用户不能为空")); existing.setText(body); existing.setStatus(nextStatus);
        commentMapper.adminUpdate(existing);
        int newContribution = Integer.valueOf(0).equals(nextStatus) ? Constants.CommentRankScore : 0;
        if (newContribution != oldContribution) {
            snapScoreService.adjustScore(snapId, newContribution - oldContribution);
        }
        return getSuccessResponseVO(commentMapper.selectByCommentId(commentId));
    }

    @DeleteMapping("/comments")
    @Transactional(rollbackFor = Exception.class)
    public ResponseVO deleteComment(@RequestParam Integer commentId, HttpServletRequest request) {
        require(request);
        CommentInfo comment = commentMapper.selectByCommentId(commentId);
        if (comment == null) throw new BusinessException("评论不存在");
        commentMapper.updateStatus(commentId, 1);
        if (Integer.valueOf(0).equals(comment.getStatus())) {
            snapScoreService.adjustScore(comment.getSnapId(), -Constants.CommentRankScore);
        }
        return getSuccessResponseVO(true);
    }

    private void require(HttpServletRequest request) { adminAuthService.require(request); }
    private Page page(Integer page, Integer size) {
        int current = page == null || page < 1 ? 1 : Math.min(page, 100000);
        int limit = size == null || size < 1 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
        return new Page(current, limit);
    }
    private Integer normalizeStatus(Integer status) { return status == null || status < 0 || status > 2 ? null : status; }
    private Integer normalizeSnapStatus(Integer status) { return status == null || status < 0 || status > 2 ? null : status; }
    private Integer normalizeCommentStatus(Integer status) { return status == null || status < 0 || status > 1 ? null : status; }
    private Integer normalizeType(Integer type) { return type == null || type < 0 || type > 4 ? null : type; }
    private String clean(String value) { return StringUtils.hasText(value) ? value.trim() : null; }
    private String required(String value, String message) { if (!StringUtils.hasText(value)) throw new BusinessException(message); return value.trim(); }
    private void validateSex(Integer sex) { if (sex != null && sex != -1 && sex != 0 && sex != 1) throw new BusinessException("性别选项无效"); }

    private Map<String, Object> userView(User user) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("userId", user.getUserId()); view.put("loginName", user.getLoginName()); view.put("name", user.getName());
        view.put("status", user.getStatus()); view.put("description", user.getDescription()); view.put("sex", user.getSex());
        view.put("major", user.getMajor()); view.put("ctime", user.getCtime());
        return view;
    }
    private <T> Map<String, Object> pageResult(List<T> items, Integer total, Page page) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items == null ? new ArrayList<>() : items); result.put("total", total == null ? 0 : total);
        result.put("page", page.number); result.put("size", page.size);
        return result;
    }
    private String hashPassword(String password) {
        try {
            byte[] salt = new byte[16]; RANDOM.nextBytes(salt); byte[] hash = derive(password, salt);
            return Base64.getEncoder().encodeToString(salt) + ":" + Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) { throw new IllegalStateException("密码加密失败", e); }
    }
    private byte[] derive(String password, byte[] salt) throws Exception {
        javax.crypto.spec.PBEKeySpec spec = new javax.crypto.spec.PBEKeySpec(password.toCharArray(), salt, 120000, 256);
        try { return javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded(); }
        finally { spec.clearPassword(); }
    }
    private static class Page {
        private final int number; private final int size; private final int offset;
        private Page(int number, int size) { this.number = number; this.size = size; this.offset = (number - 1) * size; }
    }
}
