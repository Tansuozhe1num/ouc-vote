package com.vote.mappers;

import com.vote.data.po.CommentInfo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface CommentInfoMapper<T, P> extends BaseMapper<T, P> {

    List<CommentInfo> selectBySnapId(@Param("snapId") Integer snapId,
                                     @Param("limit") Integer limit,
                                     @Param("offset") Integer offset);

    CommentInfo selectByCommentId(@Param("commentId") Integer commentId);

    List<CommentInfo> selectByUserId(@Param("userId") String userId);

    Integer updateText(@Param("commentId") Integer commentId,
                       @Param("text") String text);

    Integer updateStatus(@Param("commentId") Integer commentId,
                         @Param("status") Integer status);

    Integer deleteByCommentId(@Param("commentId") Integer commentId);
}
