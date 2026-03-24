package scau.dbksh.dto;

import java.math.BigDecimal;
import java.util.List;

public class ProductListDTO {

    private Long id;
    private String name;
    private List<String> tags;
    private BigDecimal price;
    private String imageUrl;
    private String relativeTime;
    private String description;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getRelativeTime() {
        return relativeTime;
    }

    public void setRelativeTime(String relativeTime) {
        this.relativeTime = relativeTime;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
