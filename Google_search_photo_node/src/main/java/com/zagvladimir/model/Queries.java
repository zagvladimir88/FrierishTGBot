package com.zagvladimir.model;

import lombok.Data;

import java.util.List;

@Data
public class Queries {
    private List<Request> request;
    private List<Request> nextPage;
}
