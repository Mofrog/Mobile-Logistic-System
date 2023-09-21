package com.example.logisticsystem;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class Order {
    public String Address;
    public String City;
    public String Comment;
    public String UserId;
    public String OrderDate;
    public long Status;

    public Order(){}
}
