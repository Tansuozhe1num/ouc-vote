package com.vote.query;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
public class ChildCommentInfoQuery extends BaseParam {

    private Integer cId;

    private Integer snapId;

    private List<Integer> snapIdList;

    private Integer parentId;

    private String userId;

    private String userIdFuzzy;

    private Integer status;

    private String textFuzzy;
}
