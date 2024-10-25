package com.github.glfrazier.obscanvarranger;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class CommandProperties extends Properties {

	public enum NoPropertyBehavior {
		/** Return <code>null</code> when unset properties are gotten. */
		RETURN_NULL,
		/** Throw an IllegalArgumentException when unset properties are gotten. */
		THROW_EXCEPTION,
		/**
		 * Print a stack trace to stderr and exit with an error code of -1 when unset
		 * properties are gotten.
		 */
		TERMINATE
	};

	private NoPropertyBehavior noPropertyBehavior = NoPropertyBehavior.THROW_EXCEPTION;

	public CommandProperties() {
		super();
		if (getProperty("commandproperties.behavior") != null) {
			try {
				noPropertyBehavior = NoPropertyBehavior.valueOf(getProperty("systemproperties.behavior"));
			} catch (Throwable t) {
				throw new IllegalArgumentException("Illegal value for the property 'systemproperties.behavior': '"
						+ getProperty("systemproperties.behavior") + "'\n"
						+ "  Acceptable values are: null, 'RETURN_NULL', 'THROW_EXCEPTION' or 'TERMINATE.'");
			}
		}
	}

	public CommandProperties processArguments(String[] args) {
		CommandProperties result = new CommandProperties();
		result.putAll(System.getProperties());
		for (int i = 0; i < args.length; i++) {
			String[] varValPair = args[i].split("=");
			if (varValPair.length != 2) {
				if (args[i].charAt(args[i].length() - 1) == '=') {
					String vName = varValPair[0];
					varValPair = new String[2];
					varValPair[0] = vName;
					varValPair[1] = "";
				} else {
					System.err.println("All arguments must be of the form <name>=<value>.");
					System.err.println("Argument " + i + ": " + args[i]);
					System.exit(-1);
				}
			}
			// Need code here for the parameters file
			String var = varValPair[0];
			String val = varValPair[1];
			result.setProperty(var, val);
			while (result.containsKey("propertiesfile")) {
				FileInputStream inStream;
				String fileName = result.remove("propertiesfile").toString();
				System.out.println("Loading property file <" + fileName + ">.");
				try {
					inStream = new FileInputStream(fileName);
					Properties p = new Properties();
					p.load(inStream);
					inStream.close();
					result.putAll(p);
				} catch (IOException e) {
					new Exception("Failed to load the properties file <" + fileName + ">: " + e).printStackTrace();
					System.exit(-1);
				}
			}
		}
		return result;
	}

	public Number getNumericProperty(String propName, Class<? extends Number> type) {
		if (getProperty(propName) == null) {
			return nullPropertyResponse(propName);
		}
		try {
			if (type.equals(Float.class)) {
				return Float.parseFloat(getProperty(propName));
			}
			if (type.equals(Double.class)) {
				return Double.parseDouble(getProperty(propName));
			}
			if (type.equals(Integer.class)) {
				return Integer.parseInt(getProperty(propName));
			}
			if (type.equals(Long.class)) {
				return Long.parseLong(getProperty(propName));
			}
		} catch (Throwable t) {
			throw new IllegalArgumentException(
					"Property=" + propName + ": received exception " + t + " while parsing " + getProperty(propName));
		}
		throw new IllegalArgumentException(
				"getNumericProperty only understand Double, Float, Long and Integer, not " + type);
	}

	/**
	 * The only difference between this method and getProperty(propName) is the
	 * behavior if the desired property is not set.
	 * 
	 * @param propName the name of the property
	 * @return the value of the property
	 * @see #NO_PROPERTY_BEHAVIOR
	 */
	public String getStringProperty(String propName) {
		if (getProperty(propName) == null) {
			nullPropertyResponse(propName);
			return null;
		}
		return getProperty(propName);
	}

	public float getFloatProperty(String propName) {
		return getNumericProperty(propName, Float.class).floatValue();
	}

	public int getIntProperty(String propName) {
		return getNumericProperty(propName, Integer.class).intValue();
	}

	public long getLongProperty(String propName) {
		return getNumericProperty(propName, Integer.class).longValue();
	}

	public double getDoubleProperty(String propName) {
		return getNumericProperty(propName, Double.class).doubleValue();
	}

	private Number nullPropertyResponse(String propName) {
		switch (noPropertyBehavior) {
		case RETURN_NULL:
			return null;
		case THROW_EXCEPTION:
			throw new IllegalArgumentException("Property <" + propName + "> is not set.");
		case TERMINATE:
			new IllegalArgumentException("Property <" + propName + "> is not set.").printStackTrace();
			System.exit(-1);
		}
		return null;
	}

	public Number getNumericProperty(String propName, float defaultValue) {
		if (getProperty(propName) == null) {
			return defaultValue;
		}
		return Float.parseFloat(getProperty(propName));
	}

	public Number getNumericProperty(String propName, double defaultValue) {
		if (getProperty(propName) == null) {
			return defaultValue;
		}
		return Double.parseDouble(getProperty(propName));
	}

	public Number getNumericProperty(String propName, int defaultValue) {
		if (getProperty(propName) == null) {
			return defaultValue;
		}
		return Integer.parseInt(getProperty(propName));
	}

	public Number getNumericProperty(String propName, long defaultValue) {
		if (getProperty(propName) == null) {
			return defaultValue;
		}
		return Long.parseLong(getProperty(propName));
	}

	public float getFloatProperty(String propName, float defaultValue) {
		return getNumericProperty(propName, defaultValue).floatValue();
	}

	public int getIntProperty(String propName, int defaultValue) {
		return getNumericProperty(propName, defaultValue).intValue();
	}

	public long getLongProperty(String propName, int defaultValue) {
		return getNumericProperty(propName, defaultValue).longValue();
	}

	public double getDoubleProperty(String propName, double defaultValue) {
		return getNumericProperty(propName, defaultValue).doubleValue();
	}

	public boolean getBooleanProperty(String propName) {
		String b = getStringProperty(propName);
		return stringToBoolean(b);
	}

	public boolean getBooleanProperty(String propName, boolean defaultValue) {
		String b = getProperty(propName, "");
		if (b.equals("")) {
			return defaultValue;
		}
		return stringToBoolean(b);
	}

	private boolean stringToBoolean(String b) {
		if (b.equalsIgnoreCase("true") || b.equalsIgnoreCase("t") || b.equalsIgnoreCase("1")) {
			return true;
		}
		if (b.equalsIgnoreCase("false") || b.equalsIgnoreCase("f") || b.equalsIgnoreCase("0")) {
			return false;
		}
		throw new IllegalArgumentException("<" + b + "> cannot be interpreted as boolean.");
	}

	public void setNoPropertyBehavior(NoPropertyBehavior behavior) {
		noPropertyBehavior = behavior;
	}
}
