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
public class Orders {
	// 18 fields
	int prestashopOrderId;
	int id_address_delivery;
	int id_address_invoice;
	int id_cart;
	int id_currency;
	int id_lang;
	int id_customer;
	int id_carrier;
	int current_state; //status-select
	int company; //NOT MAPPED STATIC VALUE(1)
	String module; //paymentMode
	String invoice_date;
	String payment; //Not needed String
	String date_add; //Creation date
	BigDecimal total_paid; //Total paid & incl Tax
	BigDecimal total_paid_tax_excl; //excl Tax
	String reference; //external reference
	String associations; //for salesOrderLine
	
	public int getPrestashopOrderId() {
		return prestashopOrderId;
	}
	@XmlElement
	public void setPrestashopOrderId(int prestashopOrderId) {
		this.prestashopOrderId = prestashopOrderId;
	}
	public int getId_address_delivery() {
		return id_address_delivery;
	}
	@XmlElement
	public void setId_address_delivery(int id_address_delivery) {
		this.id_address_delivery = id_address_delivery;
	}
	public int getId_address_invoice() {
		return id_address_invoice;
	}
	@XmlElement
	public void setId_address_invoice(int id_address_invoice) {
		this.id_address_invoice = id_address_invoice;
	}
	public int getId_cart() {
		return id_cart;
	}
	@XmlElement
	public void setId_cart(int id_cart) {
		this.id_cart = id_cart;
	}
	public int getId_currency() {
		return id_currency;
	}
	@XmlElement
	public void setId_currency(int id_currency) {
		this.id_currency = id_currency;
	}
	public int getId_lang() {
		return id_lang;
	}
	@XmlElement
	public void setId_lang(int id_lang) {
		this.id_lang = id_lang;
	}
	public int getId_customer() {
		return id_customer;
	}
	@XmlElement
	public void setId_customer(int id_customer) {
		this.id_customer = id_customer;
	}
	public int getId_carrier() {
		return id_carrier;
	}
	@XmlElement
	public void setId_carrier(int id_carrier) {
		this.id_carrier = id_carrier;
	}
	public int getCurrent_state() {
		return current_state;
	}
	@XmlElement
	public void setCurrent_state(int current_state) {
		this.current_state = current_state;
	}
	public String getModule() {
		return module;
	}
	@XmlElement
	public void setModule(String module) {
		this.module = module;
	}
	public int getCompany() {
		return company;
	}
	@XmlElement
	public void setCompany(int company) {
		this.company = company;
	}
	public String getInvoice_date() {
		return invoice_date;
	}
	@XmlElement
	public void setInvoice_date(String invoice_date) {
		this.invoice_date = invoice_date;
	}
	public String getPayment() {
		return payment;
	}
	@XmlElement
	public void setPayment(String payment) {
		this.payment = payment;
	}
	public String getDate_add() {
		return date_add;
	}
	@XmlElement
	public void setDate_add(String date_add) {
		this.date_add = date_add;
	}
	public BigDecimal getTotal_paid() {
		return total_paid;
	}
	@XmlElement
	public void setTotal_paid(BigDecimal total_paid) {
		this.total_paid = total_paid;
	}
	public BigDecimal getTotal_paid_tax_excl() {
		return total_paid_tax_excl;
	}
	@XmlElement
	public void setTotal_paid_tax_excl(BigDecimal total_paid_tax_excl) {
		this.total_paid_tax_excl = total_paid_tax_excl;
	}
	public String getReference() {
		return reference;
	}
	@XmlElement
	public void setReference(String reference) {
		this.reference = reference;
	}
	public String getAssociations() {
		return associations;
	}
	@XmlElement
	public void setAssociations(String associations) {
		this.associations = associations;
	}
}
