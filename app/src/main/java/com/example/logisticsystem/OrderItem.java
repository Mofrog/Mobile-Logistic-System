package com.example.logisticsystem;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.Objects;

@SuppressLint("ViewConstructor")
public class OrderItem extends LinearLayout {
    public Order CurrentOrder;
    public double Distance;
    public long Id;

    public OrderItem(@NonNull MainActivity context, Order order, long id) {
        super(context.getBaseContext());
        CurrentOrder = order;
        Id = id;

        LayoutInflater inflater = (LayoutInflater) context.getBaseContext()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.order_item, this);

        ((TextView) findViewById(R.id.txtDir))
                .setText(String.format("%s, %s", CurrentOrder.Address, CurrentOrder.City));
        ((TextView) findViewById(R.id.txtDate)).setText(String.format(CurrentOrder.OrderDate));

        ((TextView) findViewById(R.id.txtDetailDir))
                .setText(String.format("%s, %s", CurrentOrder.Address, CurrentOrder.City));
        String comment = Objects.equals(CurrentOrder.Comment, "null")
                ? "Комментарий отсутствует" : CurrentOrder.Comment;
        ((TextView) findViewById(R.id.txtComment))
                .setText(String.format("Дата: %s - %s", CurrentOrder.OrderDate, comment));

        findViewById(R.id.root).setOnClickListener(view -> context.selectItem(this));

        findViewById(R.id.btnSelect).setOnClickListener(view ->
                context.routingStart(CurrentOrder.City + ", " + CurrentOrder.Address));
    }

    public void Activate() {
        findViewById(R.id.enabled).setVisibility(View.VISIBLE);
        findViewById(R.id.disabled).setVisibility(View.GONE);
    }

    public void Disactivate() {
        findViewById(R.id.enabled).setVisibility(View.GONE);
        findViewById(R.id.disabled).setVisibility(View.VISIBLE);
    }
}
