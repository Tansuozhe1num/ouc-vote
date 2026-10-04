package com.vote.data.dto;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class SnapInfoDTO {

    public String SnapId;

    public String SnapImage;

    public Integer sex;

    public String name;

    public String qq;

    public String wx;

    public Integer age;

    public String major;

    public Long ctime;

    public Long utime;
}
