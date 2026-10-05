package com.vote.mappers;

import com.vote.data.po.ChildCommentInfo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ChildCommentInfoMapper<T, P> extends BaseMapper<T, P> {

    List<ChildCommentInfo> selectByParentId(@Param("parentId") Integer parentId,
                                            @Param("limit") Integer limit,
                                            @Param("offset") Integer offset);

    List<ChildCommentInfo> selectBySnapId(@Param("snapId") Integer snapId,
                                          @Param("limit") Integer limit,
                                          @Param("offset") Integer offset);

    ChildCommentInfo selectByCId(@Param("cId") Integer cId);

    Integer updateText(@Param("cId") Integer cId,
                       @Param("text") String text);

    Integer updateStatus(@Param("cId") Integer cId,
                         @Param("status") Integer status);

    Integer deleteByCId(@Param("cId") Integer cId);
}
