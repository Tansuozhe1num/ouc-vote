package com.vote.query;

import java.util.List;

public class SnapInfoQuery extends BaseParam {
    private String snapId;

    private List<String> snapIdList;

    private String snapIdFuzzy;

    private String name;

    private Integer sex;

    private Integer age;

    private Integer status;

    private Long ctime;

    private Long utime;

    private String major;

    public String getSnapId() {
        return snapId;
    }

    public void setSnapId(String snapId) {
        this.snapId = snapId;
    }

    public List<String> getSnapIdList() {
        return snapIdList;
    }

    public void setSnapIdList(List<String> snapIdList) {
        this.snapIdList = snapIdList;
    }

    public String getSnapIdFuzzy() {
        return snapIdFuzzy;
    }

    public void setSnapIdFuzzy(String snapIdFuzzy) {
        this.snapIdFuzzy = snapIdFuzzy;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getSex() {
        return sex;
    }

    public void setSex(Integer sex) {
        this.sex = sex;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Long getCtime() {
        return ctime;
    }

    public void setCtime(Long ctime) {
        this.ctime = ctime;
    }

    public Long getUtime() {
        return utime;
    }

    public void setUtime(Long utime) {
        this.utime = utime;
    }

    public String getMajor() {
        return major;
    }

    public void setMajor(String major) {
        this.major = major;
    }
}
