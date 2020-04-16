package com.hubber;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Set;

@Entity(name = "EntityProduct")
@Table(name = "Product", uniqueConstraints = {
        @UniqueConstraint(columnNames = "ID")
})
public class EntityProduct implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", unique = true, nullable = false)
    private long id;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "ARTICLE")
    private String article;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "PRODUCT_ID")
    private Set<EntityHistory> historyEntitySet;

    @ManyToOne
    private EntityCategory category;

    public EntityProduct() {

    }

    public EntityProduct(String description, String article) {
        this.description = description;
        this.article = article;
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

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setHistoryEntitySet(Set<EntityHistory> set) {
        this.historyEntitySet = set;
    }

    public Set<EntityHistory> getHistoryEntity() {
        return historyEntitySet;
    }

    public void setCategory(EntityCategory category) {
        this.category = category;
    }

    public EntityCategory getCategory() {
        return category;
    }

    @Override
    public String toString() {
        return "EntityProduct. Id: " + getId() + "; Article: " + getArticle() +
                "; Description: " + getDescription() + "; Category: {" + getCategory().getTitle() + "}";
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || this.getClass() != other.getClass()) return false;
        EntityProduct that = (EntityProduct) other;
        return article.equals(that.getArticle()) &&
                description.equals(that.getDescription());
    }

    @Override
    public int hashCode() {
        return 31 * article.hashCode() * description.hashCode();
    }
}
