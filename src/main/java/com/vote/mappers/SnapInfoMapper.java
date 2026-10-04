package com.vote.mappers;

import com.vote.data.po.SnapInfo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SnapInfoMapper<T, P> extends BaseMapper<T, P> {

    List<SnapInfo> selectSnapInfoList(@Param("limit") Integer limit, @Param("offset") Integer offset);

    SnapInfo selectByName(@Param("name") String name);
}
