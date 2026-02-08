package kr.co.cerberus.global.common;

public interface BaseEnum<T> {
    T getCode();
    String getDescription();
}