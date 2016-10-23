package com.soft.edi.pricewatcher;

/**
 * Created by Edi on 24.01.2016.
 */
public class Product {

    private String name;

    private String link;

    private String price;

    public Product() {
    }

    public Product(String name, String link, String price) {
        this.name = name;
        this.link = link;
        this.price = price;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getLink() {
        return link;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getPrice() {
        return price;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name + " - " + price;
    }
}
