package paymentgateway.entities;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.io.Serializable;

public class PgEntriesEntityPK implements Serializable {
    @Column(name = "sid")
    @Id
    private int sid;
    @Column(name = "amount")
    @Id
    private double amount;
    @Column(name = "nounce")
    @Id
    private int nounce;
    @Column(name = "pkc")
    @Id
    private String pkc;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PgEntriesEntityPK that = (PgEntriesEntityPK) o;

        if (sid != that.sid) return false;
        if (Double.compare(that.amount, amount) != 0) return false;
        if (nounce != that.nounce) return false;
        if (pkc != null ? !pkc.equals(that.pkc) : that.pkc != null) return false;

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
        return result;
    }
}
