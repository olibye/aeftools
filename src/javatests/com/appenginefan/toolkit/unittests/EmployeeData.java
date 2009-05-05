package com.appenginefan.toolkit.unittests;

import java.util.Date;
import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

/**
 * A simple data class as shown on
 * http://code.google.com/appengine
 * /docs/java/datastore/dataclasses.html
 */
@PersistenceCapable(identityType = IdentityType.APPLICATION)
public class EmployeeData {
  @PrimaryKey
  @Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
  private Long id;

  @Persistent
  private String firstName;

  @Persistent
  private String lastName;

  @Persistent
  private Date hireDate;

  public EmployeeData(String firstName, String lastName,
      Date hireDate) {
    this.firstName = firstName;
    this.lastName = lastName;
    this.hireDate = hireDate;
  }

  // Accessors for the fields. JDO doesn't use these, but
  // your application does.

  public Long getId() {
    return id;
  }

  public String getFirstName() {
    return firstName;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  public Date getHireDate() {
    return new Date(hireDate.getTime());
  }

  public void setHireDate(Date hireDate) {
    this.hireDate = hireDate;
  }
}