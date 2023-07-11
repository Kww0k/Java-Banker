package com.banker.entity;

import lombok.Data;

import java.util.List;

/**
 * @Author: Silvery
 * @Date: 2023/7/6 15:42
 */
@Data
public class AddRequestDto {
    Integer processNum;
    List<Integer> requestList;
}
