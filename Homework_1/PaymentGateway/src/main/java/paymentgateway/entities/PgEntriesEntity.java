package paymentgateway.entities;

import javax.persistence.*;

@Entity
@Table(name = "pg_entries", schema = "sc")
@IdClass(PgEntriesEntityPK.class)
public class PgEntriesEntity {
    @Id
    @Column(name = "sid")
    private int sid;
    @Id
    @Column(name = "amount")
    private double amount;
    @Id
    @Column(name = "nounce")
    private int nounce;
    @Id
    @Column(name = "pkc")
    private String pkc;
    @Basic
    @Column(name = "merchant")
    private String merchant;

    public int getSid() {
        return sid;
    }

    public void setSid(int sid) {
        this.sid = sid;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public int getNounce() {
        return nounce;
    }

    public void setNounce(int nounce) {
        this.nounce = nounce;
    }

    public String getPkc() {
        return pkc;
    }

    public void setPkc(String pkc) {
        this.pkc = pkc;
    }

    public String getMerchant() {
        return merchant;
    }

    public void setMerchant(String merchant) {
        this.merchant = merchant;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PgEntriesEntity that = (PgEntriesEntity) o;

        if (sid != that.sid) return false;
        if (Double.compare(that.amount, amount) != 0) return false;
        if (nounce != that.nounce) return false;
        if (pkc != null ? !pkc.equals(that.pkc) : that.pkc != null) return false;
        if (merchant != null ? !merchant.equals(that.merchant) : that.merchant != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = sid;
        temp = Double.doubleToLongBits(amount);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + nounce;
        result = 31 * result + (pkc != null ? pkc.hashCode() : 0);
        result = 31 * result + (merchant != null ? merchant.hashCode() : 0);
        return result;
    }
}
