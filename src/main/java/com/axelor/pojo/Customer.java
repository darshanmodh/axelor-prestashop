/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2012-2014 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.axelor.pojo;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.joda.time.LocalDate;

@XmlRootElement
public class Customer {

	String company;
	String firstname;
	String lastname;
	String passwd;
	String email;
	String birthday;
	int active;
	int deleted;
	int prestashopid;
	int id_default_group;
	String associations;
	String date_upd;

	public String getCompany() {
		return company;
	}

	@XmlElement
	public void setCompany(String company) {
		this.company = company;
	}

	public String getFirstname() {
		return firstname;
	}

	@XmlElement
	public void setFirstname(String firstname) {
		this.firstname = firstname;
	}

	public String getLastname() {
		return lastname;
	}

	@XmlElement
	public void setLastname(String lastname) {
		this.lastname = lastname;
	}

	public String getPasswd() {
		return passwd;
	}

	@XmlElement
	public void setPasswd(String passwd) {
		this.passwd = passwd;
	}

	public String getEmail() {
		return email;
	}

	@XmlElement
	public void setEmail(String email) {
		this.email = email;
	}

	public String getBirthday() {
		return birthday;
	}

	@XmlElement
	public void setBirthday(String date) {
		this.birthday = date;
	}

	public int getActive() {
		return active;
	}

	@XmlElement
	public void setActive(int active) {
		this.active = active;
	}

	public int getDeleted() {
		return deleted;
	}

	@XmlElement
	public void setDeleted(int deleted) {
		this.deleted = deleted;
	}

	public int getPrestashopid() {
		return prestashopid;
	}

	@XmlElement
	public void setPrestashopid(int prestashopid) {
		this.prestashopid = prestashopid;
	}

	public int getId_default_group() {
		return id_default_group;
	}

	@XmlElement
	public void setId_default_group(int id_default_group) {
		this.id_default_group = id_default_group;
	}

	public String getAssociations() {
		return associations;
	}

	@XmlElement
	public void setAssociations(String associations) {
		this.associations = associations;
	}

	public String getDate_upd() {
		return date_upd;
	}

	public void setDate_upd(String date_upd) {
		this.date_upd = date_upd;
	}

	
	
}
