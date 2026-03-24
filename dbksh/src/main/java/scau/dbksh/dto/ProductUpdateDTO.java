package scau.dbksh.dto;

public class ProductUpdateDTO extends ProductCreateDTO {

    private String status;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
