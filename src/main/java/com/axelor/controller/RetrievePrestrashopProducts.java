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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.axelor.app.prestashop.db.PrestashopConfiguration;
import com.axelor.apps.account.db.PaymentMode;
import com.axelor.apps.base.db.Address;
import com.axelor.apps.base.db.Company;
import com.axelor.apps.base.db.Currency;
import com.axelor.apps.base.db.Partner;
import com.axelor.apps.base.db.Product;
import com.axelor.apps.supplychain.db.SalesOrder;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.google.inject.persist.Transactional;

public class RetrievePrestrashopProducts {

	PrestashopConfiguration config = PrestashopConfiguration.all().fetchOne();
	String apiKey = config.getApiKey();

	@SuppressWarnings("finally")
	@Transactional
	public void syncPrestashopProducts(ActionRequest request,
			ActionResponse response) {
		String message = "";
		try {
			List<Integer> prestashopIdList = new ArrayList<Integer>();
			List<Integer> erpIdList = new ArrayList<Integer>();
			List<Product> erpList = Product.all().fetch();

			for (Product prestahopProduct : erpList) {
				erpIdList.add(prestahopProduct.getPrestashopProductId());
			}

			URL url = new URL(
					"http://localhost/client-lib/crud/action.php?resource=products&action=getallid&Akey="
							+ apiKey);
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
			System.out.println("From Prestashop Products :: "
					+ prestashopIdList.size());
			System.out.println("From ERP Products :: " + erpIdList.size());
			scan.close();

			// Check new entries in the prestashop
			Iterator<Integer> prestaListIterator = prestashopIdList.iterator();
			while (prestaListIterator.hasNext()) {
				Integer tempId = prestaListIterator.next();
				System.out
						.println("Current PrestashopProductID for operation ::"
								+ tempId);
				if (erpIdList.contains(tempId)) {
					erpIdList.remove(tempId);
				} else {
					System.out
							.println("Current PrestashopProductID for insertion operation ::"
									+ tempId);
					// insert new data in ERP Database
					insertProducts(tempId);
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
					if (tempId != 0) {
						Product productDelete = Product.all()
								.filter("prestashop_product_id=?", tempId)
								.fetchOne();
						String name = productDelete.getName();
						productDelete.remove();
						System.out.println("customer deleted ::" + name);
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
			message = "Wrong Authentication Key or Key has been disabled.";
		} finally {
			response.setFlash(message);
		}
	}

	@Transactional
	public void insertProducts(int prestashopProductId) {
		com.axelor.pojo.Products pojoProduct = new com.axelor.pojo.Products();
		// SET DEFAULT VALUES
		pojoProduct.setPrestashopProductId(0);
		pojoProduct.setPrice(new BigDecimal("00.00"));
		pojoProduct.setWholesale_price(new BigDecimal("00.00"));
		pojoProduct.setName("PRODUCT_NAME");
		pojoProduct.setReference("PRODUCT_REFERENCE");
		pojoProduct.setDescription("PRODUCT_DESCRIPTION");
		//pojoProduct.setId_default_image("IMAGE".getBytes());

		try {
			URL url = new URL(
					"http://localhost/client-lib/crud/action.php?resource=products&action=retrieve&id="
							+ prestashopProductId + "&Akey=" + apiKey);
			HttpURLConnection connection = (HttpURLConnection) url
					.openConnection();

			connection.setRequestMethod("POST");
			JAXBContext jaxbContext = JAXBContext
					.newInstance(com.axelor.pojo.Products.class);
			Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

			jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			connection.setDoOutput(true);
			connection.setDoInput(true);

			OutputStream outputStream = connection.getOutputStream();
			jaxbMarshaller.marshal(pojoProduct, outputStream);
			connection.connect();

			Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
			pojoProduct = (com.axelor.pojo.Products) jaxbUnmarshaller
					.unmarshal(new UnmarshalInputStream(connection
							.getInputStream()));
			System.out.println("Unmarshaling complete");
			String code = pojoProduct.getReference();

			Product checkProduct = Product.all().filter("code=?", code)
					.fetchOne();
			if (checkProduct != null) {
				checkProduct.setPrestashopProductId(prestashopProductId);
				checkProduct.save();
			} else {
				Product prestashopProduct = new Product();

				// Resolve product picture
				 /* File file = new File("t.jpeg"); 
				  FileOutputStream fos = new FileOutputStream(file); 
				  URL pictureUrl = new URL("http://"+apiKey +"@localhost/prestashop/api/images/products/5/7");
				  HttpURLConnection pictureConnection = (HttpURLConnection) pictureUrl.openConnection();
				  pictureConnection.setRequestMethod("POST");
				  InputStream pictureInputStream = pictureConnection.getInputStream();
				  ByteArrayOutputStream output = new ByteArrayOutputStream();
				  byte [] buffer = new byte[1024]; 
				  int n = 0; 
				  while (-1 !=(n=pictureInputStream.read(buffer))) { 
					  output.write(buffer,0, n);
				  }
				  pictureInputStream.close(); 
				  byte [] data = output.toByteArray(); 
				  fos.write(data); 
				  fos.close();
				 */

				// add this product into ERP
				prestashopProduct.setPrestashopProductId(prestashopProductId);
				prestashopProduct.setSalePrice(pojoProduct.getPrice());
				prestashopProduct.setWholesalePrice(pojoProduct
						.getWholesale_price());
				prestashopProduct.setName(pojoProduct.getName());
				prestashopProduct.setCode(pojoProduct.getReference());
				prestashopProduct.setDescription(pojoProduct.getDescription());
				//prestashopProduct.setPicture(data);

				// default values in ERP
				prestashopProduct.setApplicationTypeSelect(1);
				prestashopProduct.setSaleSupplySelect(1);
				prestashopProduct.setProcurementMethodSelect("buy");
				prestashopProduct.setProductTypeSelect("storable");

				prestashopProduct.save();
			}
		} catch (Exception e) {
			System.out.println(e);
		}
	}
}
