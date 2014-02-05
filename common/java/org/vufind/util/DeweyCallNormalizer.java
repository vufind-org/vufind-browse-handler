package org.vufind.util;

import org.solrmarc.callnum.DeweyCallNumber;

public class DeweyCallNormalizer implements Normalizer {

	
	@Override
	public byte[] normalize(String s) 
	{
		return new DeweyCallNumber(s).getShelfKey().getBytes();
	}

}
