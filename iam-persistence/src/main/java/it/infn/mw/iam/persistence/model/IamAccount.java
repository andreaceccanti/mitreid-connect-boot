package it.infn.mw.iam.persistence.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.TemporalType;
import javax.persistence.Temporal;
import javax.validation.constraints.NotNull;

/**
 * 
 *
 */
@Entity
@Table(name = "iam_account")
public class IamAccount {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 36, unique = true)
  private String uuid;

  @Column(nullable = false, length = 128, unique = true)
  @NotNull
  private String username;

  @Column(length = 128)
  private String password;

  @Temporal(TemporalType.TIMESTAMP)
  @Column(nullable = false)
  Date creationTime;

  @Temporal(TemporalType.TIMESTAMP)
  @Column(nullable = false)
  Date lastUpdateTime;

  @OneToOne(cascade=CascadeType.ALL)
  @JoinColumn(name = "user_info_id")
  private IamUserInfo userInfo;

  @ManyToMany
  @JoinTable(name = "iam_account_authority",
    joinColumns = @JoinColumn(name = "account_id",
      referencedColumnName = "id") ,
    inverseJoinColumns = @JoinColumn(name = "authority_id",
      referencedColumnName = "id") )
  private Set<IamAuthority> authorities = new HashSet<>();

  @ManyToMany
  @JoinTable(name = "iam_account_group",
    joinColumns = @JoinColumn(name = "account_id",
      referencedColumnName = "id") ,
    inverseJoinColumns = @JoinColumn(name = "group_id",
      referencedColumnName = "id") )
  private Set<IamGroup> groups = new HashSet<>();

  @OneToMany(mappedBy = "account", cascade=CascadeType.ALL)
  private List<IamSamlId> samlIds = new ArrayList<>();

  @OneToMany(mappedBy = "account", cascade=CascadeType.ALL)
  private List<IamOidcId> oidcIds = new ArrayList<>();

  @OneToMany(mappedBy = "account", cascade=CascadeType.ALL)
  private List<IamSshKey> sshKeys = new ArrayList<>();

  @OneToMany(mappedBy = "account", cascade=CascadeType.ALL)
  private List<IamX509Certificate> x509Certificates = new ArrayList<>();

  public IamAccount() {
  }

  public Long getId() {

    return id;
  }

  public void setId(final Long id) {

    this.id = id;
  }

  public String getUuid() {

    return uuid;
  }

  public void setUuid(final String uuid) {

    this.uuid = uuid;
  }

  public String getUsername() {

    return username;
  }

  public void setUsername(final String username) {

    this.username = username;
  }

  public String getPassword() {

    return password;
  }

  public void setPassword(final String password) {

    this.password = password;
  }

  public IamUserInfo getUserInfo() {

    return userInfo;
  }

  public void setUserInfo(final IamUserInfo userInfo) {

    this.userInfo = userInfo;
  }

  public Set<IamAuthority> getAuthorities() {

    return authorities;
  }

  public void setAuthorities(final Set<IamAuthority> authorities) {

    this.authorities = authorities;
  }

  public Set<IamGroup> getGroups() {

    return groups;
  }

  public void setGroups(final Set<IamGroup> groups) {

    this.groups = groups;
  }

  public Date getCreationTime() {

    return creationTime;
  }

  public void setCreationTime(Date creationTime) {

    this.creationTime = creationTime;
  }

  public Date getLastUpdateTime() {

    return lastUpdateTime;
  }

  public void setLastUpdateTime(Date lastUpdateTime) {

    this.lastUpdateTime = lastUpdateTime;
  }

  public List<IamSamlId> getSamlIds() {

    return samlIds;
  }

  public void setSamlIds(List<IamSamlId> samlIds) {

    this.samlIds = samlIds;
  }

  public List<IamOidcId> getOidcIds() {

    return oidcIds;
  }

  public void setOidcIds(List<IamOidcId> oidcIds) {

    this.oidcIds = oidcIds;
  }

  public List<IamSshKey> getSshKeys() {

    return sshKeys;
  }

  public void setSshKeys(List<IamSshKey> sshKeys) {

    this.sshKeys = sshKeys;
  }

  public List<IamX509Certificate> getX509Certificates() {

    return x509Certificates;
  }

  public void setX509Certificates(List<IamX509Certificate> x509Certificates) {

    this.x509Certificates = x509Certificates;
  }

  @Override
  public int hashCode() {

    final int prime = 31;
    int result = 1;
    result = prime * result + ((uuid == null) ? 0 : uuid.hashCode());
    return result;
  }

  @Override
  public boolean equals(final Object obj) {

    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    IamAccount other = (IamAccount) obj;
    if (uuid == null) {
      if (other.uuid != null)
        return false;
    } else if (!uuid.equals(other.uuid))
      return false;
    return true;
  }

}