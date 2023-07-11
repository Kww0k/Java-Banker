package com.banker.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: Silvery
 * @Date: 2023/7/5 17:03
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RestBean<T> {
    int code;
    T data;
}
