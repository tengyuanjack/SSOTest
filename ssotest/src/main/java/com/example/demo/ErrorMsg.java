package com.example.demo;

import lombok.Getter;
import lombok.ToString;

/**
 * @author zhaoteng
 * @date 2020/3/15
 */
@Getter
public class ErrorMsg {

    private String msg;

    public ErrorMsg(String msg) {
        this.msg = "Error: " + msg;
    }

    @Override
    public String toString() {
        return msg;
    }
}
