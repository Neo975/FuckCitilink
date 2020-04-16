package com.hubber;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.util.Objects;

public class ProxyProduct {

    private String description;
    private String article;
    private Float price;
    private String category;

    public ProxyProduct(String description, String article, Float price, String category) {
        this.description = description;
        this.article = article;
        this.price = price;
        this.category = category;
    }

    public ProxyProduct(String jsonString) {
        JSONObject jsonObject = (JSONObject) JSONValue.parse(jsonString);
        Float price = ((Long) jsonObject.get("price")).floatValue();
        this.article = (String) jsonObject.get("id");
        this.category = (String) jsonObject.get("categoryName");
        this.description = (String) jsonObject.get("shortName");;
        this.price = price;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public void setArticle(String article) {
        this.article = article;
    }

    public String getArticle() {
        return article;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getCategory() {
        return category;
    }

    public void setPrice(Float price) {
        this.price = price;
    }

    public Float getPrice() {
        return price;
    }

    @Override
    public String toString() {
        return "ProxyProduct. Article: " + article + "; Description: " +
                description + "; Price: " + price + "; Category " + category;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProxyProduct that = (ProxyProduct) o;
        return Objects.equals(description, that.description) &&
                article.equals(that.article) &&
                price.equals(that.price) &&
                Objects.equals(category, that.category);
    }

    @Override
    public int hashCode() {
        return Objects.hash(description, article, price, category);
    }
}
