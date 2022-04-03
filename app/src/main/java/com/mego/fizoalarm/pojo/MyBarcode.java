package com.mego.fizoalarm.pojo;

import java.io.Serializable;

public class MyBarcode implements Serializable {

    private String label;
    private String code;

    public MyBarcode(String label, String code) {
        this.label = label;
        this.code = code;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
