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

import java.math.BigDecimal;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Products {
	// 7 fields
	int prestashopProductId;
	BigDecimal price;
	BigDecimal wholesale_price;
	String name;
	String reference;
	String description;
	byte[] id_default_image;
	
	public int getPrestashopProductId() {
		return prestashopProductId;
	}
	@XmlElement
	public void setPrestashopProductId(int prestashopProductId) {
		this.prestashopProductId = prestashopProductId;
	}
	public BigDecimal getPrice() {
		return price;
	}
	@XmlElement
	public void setPrice(BigDecimal price) {
		this.price = price;
	}
	public BigDecimal getWholesale_price() {
		return wholesale_price;
	}
	@XmlElement
	public void setWholesale_price(BigDecimal wholesale_price) {
		this.wholesale_price = wholesale_price;
	}
	public String getName() {
		return name;
	}
	@XmlElement
	public void setName(String name) {
		this.name = name;
	}
	public String getReference() {
		return reference;
	}
	@XmlElement
	public void setReference(String reference) {
		this.reference = reference;
	}
	public String getDescription() {
		return description;
	}
	@XmlElement
	public void setDescription(String description) {
		this.description = description;
	}
	public byte[] getId_default_image() {
		return id_default_image;
	}
	public void setId_default_image(byte[] id_default_image) {
		this.id_default_image = id_default_image;
	}	
}
