package com.hubber;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity(name = "EntityHistory")
@Table(name = "HISTORY", uniqueConstraints = {
        @UniqueConstraint(columnNames = "ID")
})
public class EntityHistory implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", unique = true, nullable = false)
    private Long id;

    @Basic
    @Column(name = "TIMESTAMP")
    private LocalDateTime timestamp;

    @Column(name = "PRICE")
    private Float price;

    @ManyToOne
    private EntityProduct product;

    public EntityHistory() {

    }

    public EntityHistory(LocalDateTime timestamp, Float price) {
        this.timestamp = timestamp;
        this.price = price;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setPrice(Float price) {
        this.price = price;
    }

    public Float getPrice() {
        return price;
    }

    public void setProduct(EntityProduct product) {
        this.product = product;
    }

    public EntityProduct getProduct() {
        return product;
    }

    @Override
    public String toString() {
        return "EntityHistory. Id: " + getId() + "; Timestamp: " + getTimestamp() +
                "; Price: " + getPrice() + "; Product: {" + getProduct() + "}";
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || this.getClass() != other.getClass()) return false;
        EntityHistory that = (EntityHistory) other;
        return price.equals(that.getPrice()) &&
                timestamp.equals(that.getTimestamp()) &&
                product.equals(that.getProduct());
    }

    @Override
    public int hashCode() {
        return 31 * price.hashCode() * timestamp.hashCode() * product.hashCode();
    }
}
