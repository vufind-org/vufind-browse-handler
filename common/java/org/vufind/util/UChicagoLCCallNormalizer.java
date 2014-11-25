package org.vufind.util;

import org.solrmarc.callnum.UChicagoLCCallNumber;

public class UChicagoLCCallNormalizer implements Normalizer {

	
	@Override
	public byte[] normalize(String s) 
	{
		return new UChicagoLCCallNumber(s).getShelfKey().getBytes();
	}

}
