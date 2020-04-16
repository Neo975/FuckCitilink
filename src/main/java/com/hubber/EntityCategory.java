package com.hubber;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Set;

@Entity(name = "EntityCategory")
@Table(name = "Category", uniqueConstraints = {
        @UniqueConstraint(columnNames = "ID")
})
public class EntityCategory implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", unique = true, nullable = false)
    private long id;

    @Column(name = "TITLE")
    private String title;

    @OneToMany
    @JoinColumn(name = "CATEGORY_ID")
    private Set<EntityProduct> productSet;

    public EntityCategory() {
    }

    public EntityCategory(String title) {
        this.title = title;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public void setProductSet(Set<EntityProduct> productSet) {
        this.productSet = productSet;
    }

    public Set<EntityProduct> getProductSet() {
        return productSet;
    }

    @Override
    public String toString() {
        return "EntityCategory. Id: " + getId() + "; Title: " + getTitle();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || this.getClass() != other.getClass()) return false;
        EntityCategory that = (EntityCategory) other;
        return title.equals(that.getTitle());
    }

    @Override
    public int hashCode() {
        return 31 * title.hashCode();
    }
}
