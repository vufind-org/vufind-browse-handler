package org.vufind.util;

import org.solrmarc.callnum.LCCallNumber;

public class LCCallNormalizer implements Normalizer {

	
	@Override
	public byte[] normalize(String s) 
	{
		return new LCCallNumber(s).getShelfKey().getBytes();
	}

}
