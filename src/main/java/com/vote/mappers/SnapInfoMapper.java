package com.vote.mappers;

import com.vote.data.po.SnapInfo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SnapInfoMapper<T, P> extends BaseMapper<T, P> {

    List<SnapInfo> selectSnapInfoList(@Param("limit") Integer limit, @Param("offset") Integer offset);
    List<SnapInfo> selectSnapInfoListByType(@Param("limit") Integer limit, @Param("offset") Integer offset, @Param("type") Integer type);
    List<SnapInfo> selectByIds(@Param("ids") List<Integer> ids);
    List<SnapInfo> adminSelectList(@Param("keyword") String keyword,
                                   @Param("type") Integer type,
                                   @Param("status") Integer status,
                                   @Param("limit") Integer limit,
                                   @Param("offset") Integer offset);
    Integer adminSelectCount(@Param("keyword") String keyword,
                             @Param("type") Integer type,
                             @Param("status") Integer status);
    Integer adminCountByStatus(@Param("status") Integer status);
    SnapInfo adminSelectBySnapId(@Param("snapId") Integer snapId);
    Integer adminUpdate(SnapInfo snapInfo);
    Integer selectMaxSnapId();
    SnapInfo selectByUserId(@Param("userId") Long userId);
    List<SnapInfo> selectMine(@Param("userId") Long userId, @Param("limit") Integer limit, @Param("offset") Integer offset);
    Integer updateStatus(@Param("snapId") Integer snapId, @Param("userId") Long userId, @Param("status") Integer status);

    SnapInfo selectBySnapId(@Param("snapId") Integer snapId);
}
