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
package com.axelor.controller;

import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.joda.time.IllegalFieldValueException;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.axelor.app.prestashop.db.PrestashopConfiguration;
import com.axelor.apps.account.db.PaymentMode;
import com.axelor.apps.account.db.TaxLine;
import com.axelor.apps.base.db.Address;
import com.axelor.apps.base.db.Company;
import com.axelor.apps.base.db.Currency;
import com.axelor.apps.base.db.Partner;
import com.axelor.apps.base.db.Product;
import com.axelor.apps.base.db.Unit;
import com.axelor.apps.supplychain.db.SalesOrder;
import com.axelor.apps.supplychain.db.SalesOrderLine;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.google.inject.persist.Transactional;

public class RetrievePrestrashopOrders {
	
	PrestashopConfiguration config = PrestashopConfiguration.all().fetchOne();
	String apiKey = config.getApiKey();
	
	@SuppressWarnings("finally")
	@Transactional
	public void syncPrestashopOrders(ActionRequest request,
			ActionResponse response) {
		String message="";
		try {			
			List<Integer> prestashopIdList = new ArrayList<Integer>();
			List<Integer> erpIdList = new ArrayList<Integer>();
			List<SalesOrder> erpList = SalesOrder.all().fetch();

			for (SalesOrder prestahopOrder : erpList) {
				erpIdList.add(prestahopOrder.getPrestashopOrderId());
			}

			URL url = new URL("http://localhost/client-lib/crud/action.php?resource=orders&action=getallid&Akey=" + apiKey);
			HttpURLConnection connection = (HttpURLConnection) url
					.openConnection();
			connection.setRequestMethod("GET");
			connection.connect();

			InputStream inputStream = connection.getInputStream();
			Scanner scan = new Scanner(inputStream);
			while (scan.hasNext()) {
				String data = scan.nextLine();
				System.out.println(data);
				prestashopIdList.add(Integer.parseInt(data));
			}
			System.out.println("From Prestashop Orders :: " + prestashopIdList.size());
			System.out.println("From ERP Orders :: " + erpIdList.size());
			scan.close();

			// Check new entries in the prestashop
			Iterator<Integer> prestaListIterator = prestashopIdList.iterator();
			while (prestaListIterator.hasNext()) {
				Integer tempId = prestaListIterator.next();
				System.out.println("Current OrderPrestashopId for operation ::" + tempId);
				if (erpIdList.contains(tempId)) {
					erpIdList.remove(tempId);
				} else {
					System.out.println("Current OrderPrestashopId for insertion operation ::" + tempId);
					// insert new data in ERP Database
					insertOrders(tempId);
					erpIdList.remove(tempId);
				}
			}
			if (erpIdList.isEmpty()) {
				System.out.println("Synchronization is completed.");
				message = "Synchronization is completed.";
			} else {
				// delete from ERP
				Iterator<Integer> erpListIterator = erpIdList.iterator();
				while (erpListIterator.hasNext()) {
					Integer tempId = erpListIterator.next();
					if(tempId != 0) {
						SalesOrder orderDelete = SalesOrder.all().filter("prestashop_order_id=?", tempId).fetchOne();
						String reference = orderDelete.getExternalReference();
						orderDelete.remove();
						System.out.println("customer deleted ::" + reference);
					}
				}
				while (prestaListIterator.hasNext()) {
					Integer tempId = prestaListIterator.next();
					System.out.println("Currently in prestashop ::" + tempId);
				}
				System.out.println("Synchronization is completed.");
				message = "Synchronization is completed.";
			}
		} catch (Exception e) {
			message="Wrong Authentication Key or Key has been disabled.";
		}
		finally
		{
			response.setFlash(message);
		}
	}
	
	@Transactional
	public void insertOrders(int prestashopOrderId) {		
		System.out.println("Setting Default values...");
		com.axelor.pojo.Orders pojoOrder = new com.axelor.pojo.Orders();
		// SET DEFAULT VALUES
		pojoOrder.setPrestashopOrderId(0);
		pojoOrder.setId_address_delivery(0);
		pojoOrder.setId_address_invoice(0);
		pojoOrder.setId_cart(0);
		pojoOrder.setId_currency(0);
		pojoOrder.setId_lang(0);
		pojoOrder.setId_customer(0);
		pojoOrder.setId_carrier(0);
		pojoOrder.setCurrent_state(0);
		pojoOrder.setModule("cashondelivery");
		pojoOrder.setInvoice_date("1970-01-01");
		pojoOrder.setPayment("Cash on delivery (COD)");
		pojoOrder.setDate_add("1970-01-01");
		pojoOrder.setTotal_paid(new BigDecimal("00.00"));
		pojoOrder.setTotal_paid_tax_excl(new BigDecimal("00.00"));
		pojoOrder.setReference("REFERENCE");
		pojoOrder.setCompany(0);
		pojoOrder.setAssociations("associations");
		System.out.println("INserting............");
		try {
			URL url = new URL(
					"http://localhost/client-lib/crud/action.php?resource=orders&action=retrieve&id="
							+ prestashopOrderId + "&Akey=" + apiKey);			
			HttpURLConnection connection = (HttpURLConnection) url
					.openConnection();
			
			connection.setRequestMethod("POST");
			JAXBContext jaxbContext = JAXBContext
					.newInstance(com.axelor.pojo.Orders.class);
			Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

			jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			connection.setDoOutput(true);
			connection.setDoInput(true);
			OutputStream outputStream = connection.getOutputStream();
			jaxbMarshaller.marshal(pojoOrder, outputStream);
			connection.connect();		
			
			//InputStream inputStream = connection.getInputStream();
			//Scanner scan = new Scanner(inputStream);
			//String temp="";
			
//			while (scan.hasNext()) {
//				temp += scan.nextLine();				
//			}
//			scan.close();
		//	System.out.println(temp);
				
			Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
			pojoOrder = (com.axelor.pojo.Orders) jaxbUnmarshaller
					.unmarshal(new UnmarshalInputStream(connection
							.getInputStream()));					
			
			SalesOrder prestashopSaleOrder = new SalesOrder();
			
			//Resolve invoiceFirstDAte and creationDate
			DateTimeFormatter dateTimeFormat = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");
			LocalDate invoiceFirstDate;
			LocalDate creationDate;
			try {
				invoiceFirstDate = dateTimeFormat.parseLocalDate(pojoOrder.getInvoice_date());
			} catch(IllegalFieldValueException e) {
				invoiceFirstDate = new LocalDate("1970-01-01");
			}
			creationDate = dateTimeFormat.parseLocalDate(pojoOrder.getDate_add());
			
			//Resolve Address
			
			Address baseAddress = Address.all().filter("prestashopid=?", pojoOrder.getId_address_delivery()).fetchOne();
			
			//Resolve Currency
			Currency baseCurrency = Currency.all().filter("prestashop_currency_id=?", pojoOrder.getId_currency()).fetchOne();
			
			//Resolve Partner
			Partner basePartner = Partner.all().filter("prestashopid=?", pojoOrder.getId_customer()).fetchOne();
			
			if(baseAddress == null || basePartner == null)
				return;
			System.out.println("Currency :: " + baseCurrency.getPrestashopCurrencyId());
			//Resolve Total Tax
			BigDecimal totalPaid, totalPaidExcl, totalTax;
			totalPaid = pojoOrder.getTotal_paid();
			totalPaidExcl = pojoOrder.getTotal_paid_tax_excl();
			totalTax = totalPaid.subtract(totalPaidExcl);
			
			
			//Resolve Current State / status_select
			int statusSelect;
			int prestashopCurentState = pojoOrder.getCurrent_state();
			switch(prestashopCurentState) {
			case 1:
			case 3:
			case 10:
			case 11:
				statusSelect = 1;	//assign draft to statusSelect
				break;
			case 2:
			case 12:
				statusSelect = 2;	//assign confirmed to statusSelect
				break;
			case 4:
			case 5:
			case 7:
			case 9:
				statusSelect = 3;	//assign validated to statusSelect
				break;
			case 6:
			case 8:
				statusSelect = 4;	//assign cancelled to statusSelect
				break;
			default:
				statusSelect = 0;	//assign Error to statusSelect
			}
			
			//Resolve Payment Mode
			int erpPaymentMode = 0;
			String paymentMode = pojoOrder.getModule();
			if(paymentMode.equals("cashondelivery"))
				erpPaymentMode = 4;
			else if(paymentMode.equals("cheque"))
				erpPaymentMode = 6;
			else if(paymentMode.equals("bankwire"))
				erpPaymentMode = 8;
			PaymentMode basePaymentMode = PaymentMode.all().filter("id=?", erpPaymentMode).fetchOne();
			
			//Resolve Company, currently set to static value(1)
			Company baseCompany = Company.find(1L);
			
			// add this order into ERP
			prestashopSaleOrder.setPrestashopOrderId(prestashopOrderId);
			prestashopSaleOrder.setDeliveryAddress(baseAddress);
			prestashopSaleOrder.setMainInvoicingAddress(baseAddress);
			prestashopSaleOrder.setPrestashopCartId(pojoOrder.getId_cart());
			prestashopSaleOrder.setCurrency(baseCurrency);
			prestashopSaleOrder.setClientPartner(basePartner);
			prestashopSaleOrder.setPrestashopCarrierId(pojoOrder.getId_carrier());
			prestashopSaleOrder.setStatusSelect(statusSelect);
			prestashopSaleOrder.setPaymentMode(basePaymentMode);
			prestashopSaleOrder.setInvoicedFirstDate(invoiceFirstDate);
			prestashopSaleOrder.setPrestashopPayment(pojoOrder.getPayment());
			prestashopSaleOrder.setCreationDate(creationDate);
			prestashopSaleOrder.setTaxTotal(totalTax);
			prestashopSaleOrder.setInTaxTotal(pojoOrder.getTotal_paid());
			prestashopSaleOrder.setExTaxTotal(pojoOrder.getTotal_paid_tax_excl());
			prestashopSaleOrder.setExternalReference(pojoOrder.getReference());
			prestashopSaleOrder.setCompany(baseCompany);
			prestashopSaleOrder.save();	
			String salesOrders = pojoOrder.getAssociations();
			System.out.println("POJO ASDS ::" + salesOrders);
				
			
			String association = "";
			int count=0;
			Pattern associationsPattern = Pattern.compile("^count:(.*?);(.*?)/associations");
			Matcher associationsMatcher = associationsPattern.matcher(salesOrders);
			while (associationsMatcher.find()) {
				count = Integer.parseInt(associationsMatcher.group(1));
		        association = associationsMatcher.group(2);
		    }
			
			System.out.println("COUNT :: "+count);
			System.out.println("ASSO :: "+association);
			String[] salesOrderLineArray = new String[count];
			String[] salesOrderLine = new String[8];
			salesOrderLineArray = association.split(":::");
			for(int i=0;i<salesOrderLineArray.length;i++) {
				System.out.println("ARRAY :: " +salesOrderLineArray[i]);
				salesOrderLine = salesOrderLineArray[i].split(";;;");
				
				BigDecimal price = new BigDecimal(salesOrderLine[5]);
				BigDecimal qty = new BigDecimal(salesOrderLine[3]);
				BigDecimal exTaxTotal = price.multiply(qty);
				
				SalesOrderLine orderLines = new SalesOrderLine();
				orderLines.setCompanyExTaxTotal(exTaxTotal);
				orderLines.setExTaxTotal(exTaxTotal);
				orderLines.setPrice(price);
				orderLines.setProductName(salesOrderLine[4]);
				orderLines.setQty(qty);
				orderLines.setSaleSupplySelect(1);
				orderLines.setSequence(i+1);
				orderLines.setPrestashopSalesOrderLineId(Integer.parseInt(salesOrderLine[0]));				
				orderLines.setTaxLine(TaxLine.find(1L));
				orderLines.setUnit(Unit.find(1L));
				
				orderLines.setSalesOrder(SalesOrder.find(prestashopSaleOrder.getId()));
				
				Product productOfSalesOrderLine = Product.all().filter("prestashopProductId=?", Integer.parseInt(salesOrderLine[1])).fetchOne();
				if(productOfSalesOrderLine !=null)
				{
					System.out.println("Saving");
					orderLines.setProduct(productOfSalesOrderLine);
					orderLines.save();
				}
				else
				{
					System.out.println("Product is removed from prestashop.");
				}
				System.out.println("PRODUCT2 :: "+salesOrderLine[2]);
				System.out.println("PRODUCT3 :: "+salesOrderLine[3]);
				System.out.println("PRODUCT4 :: "+salesOrderLine[4]);
			}
		
		
			
			
			
		} catch (Exception e) {
			System.out.println(e);
		}
	}
}
