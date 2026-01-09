package org.eclipse.openvsx.entities;

import jakarta.persistence.*;

import java.util.List;
import java.util.Objects;

@Entity
public class Customer {

    @Id
    @GeneratedValue(generator = "customerSeq")
    @SequenceGenerator(name = "customerSeq", sequenceName = "customer_seq")
    private long id;

    private String name;

    @Column(length = 2048)
    @Convert(converter = ListOfStringConverter.class)
    private List<String> cidrs;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getCidrs() {
        return cidrs;
    }

    public void setCidrs(List<String> cidrs) {
        this.cidrs = cidrs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Customer that = (Customer) o;
        return id == that.id
                && Objects.equals(name, that.name)
                && Objects.equals(cidrs, that.cidrs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, cidrs);
    }
}
