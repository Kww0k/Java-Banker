package com.banker.entity;

import lombok.Data;

import java.util.List;

/**
 * @Author: Silvery
 * @Date: 2023/7/5 21:08
 */
@Data
public class FinalDto {
    List<Integer> max;
    List<Integer> allocation;
    List<Integer> need;
}
