package merchant.entities;

import javax.persistence.*;

@Entity
@Table(name = "merchant_sessions", schema = "sc")
public class MerchantSessionsEntity {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "ID")
    private int id;
    @Basic
    @Column(name = "PKC")
    private String pkc;
    @Basic
    @Column(name = "ORDER_DESC")
    private String orderDesc;
    @Basic
    @Column(name = "AMOUNT")
    private Double amount;
    @Basic
    @Column(name = "NONCE")
    private Integer nonce;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPkc() {
        return pkc;
    }

    public void setPkc(String pkc) {
        this.pkc = pkc;
    }

    public String getOrderDesc() {
        return orderDesc;
    }

    public void setOrderDesc(String orderDesc) {
        this.orderDesc = orderDesc;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public Integer getNonce() {
        return nonce;
    }

    public void setNonce(Integer nonce) {
        this.nonce = nonce;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MerchantSessionsEntity that = (MerchantSessionsEntity) o;

        if (id != that.id) return false;
        if (pkc != null ? !pkc.equals(that.pkc) : that.pkc != null) return false;
        if (orderDesc != null ? !orderDesc.equals(that.orderDesc) : that.orderDesc != null) return false;
        if (amount != null ? !amount.equals(that.amount) : that.amount != null) return false;
        if (nonce != null ? !nonce.equals(that.nonce) : that.nonce != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + (pkc != null ? pkc.hashCode() : 0);
        result = 31 * result + (orderDesc != null ? orderDesc.hashCode() : 0);
        result = 31 * result + (amount != null ? amount.hashCode() : 0);
        result = 31 * result + (nonce != null ? nonce.hashCode() : 0);
        return result;
    }
}
