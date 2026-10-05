package com.vote.query;

import java.util.List;

public class SnapInfoQuery extends BaseParam {
    private Integer snapId;

    private List<Integer> snapIdList;

    private String name;

    private String nameFuzzy;

    private Integer sex;

    private Integer age;

    private Integer status;

    private Long ctime;

    private Long utime;

    private String major;

    public Integer getSnapId() {
        return snapId;
    }

    public void setSnapId(Integer snapId) {
        this.snapId = snapId;
    }

    public List<Integer> getSnapIdList() {
        return snapIdList;
    }

    public void setSnapIdList(List<Integer> snapIdList) {
        this.snapIdList = snapIdList;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNameFuzzy() {
        return nameFuzzy;
    }

    public void setNameFuzzy(String nameFuzzy) {
        this.nameFuzzy = nameFuzzy;
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
