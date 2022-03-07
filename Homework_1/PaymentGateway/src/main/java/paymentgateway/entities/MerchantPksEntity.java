package paymentgateway.entities;

import javax.persistence.*;

@Entity
@Table(name = "merchant_pks", schema = "sc")
public class MerchantPksEntity {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "merchant")
    private String merchant;
    @Basic
    @Column(name = "pkm")
    private String pkm;

    public String getMerchant() {
        return merchant;
    }

    public void setMerchant(String merchant) {
        this.merchant = merchant;
    }

    public String getPkm() {
        return pkm;
    }

    public void setPkm(String pkm) {
        this.pkm = pkm;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MerchantPksEntity that = (MerchantPksEntity) o;

        if (merchant != null ? !merchant.equals(that.merchant) : that.merchant != null) return false;
        if (pkm != null ? !pkm.equals(that.pkm) : that.pkm != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = merchant != null ? merchant.hashCode() : 0;
        result = 31 * result + (pkm != null ? pkm.hashCode() : 0);
        return result;
    }
}
