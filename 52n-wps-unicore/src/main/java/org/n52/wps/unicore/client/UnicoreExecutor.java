
package org.n52.wps.unicore.client;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Properties;

import org.n52.wps.commons.WPSConfig;
import org.n52.wps.server.ExceptionReport;
import org.n52.wps.unicore.UnicoreAlgorithmInput;
import org.n52.wps.unicore.UnicoreAlgorithmOutput;
import org.n52.wps.unicore.UnicoreAlgorithmRepository;
import org.n52.wps.unicore.utilities.Compression;

public class UnicoreExecutor
{
	public static String METHOD_NAME = "run";
	public static Class<?>[] METHOD_PARAMETER = {
		Map.class
	};

	protected String userDirectory;
	protected Properties unicoreProperties;

	public UnicoreExecutor() throws Exception
	{
		userDirectory = System.getProperty("user.dir");
		WPSConfig.getInstance(userDirectory + "/wps_config.xml");
		unicoreProperties = UnicoreAlgorithmRepository.getInstance().getUnicoreProperties();
	}

	public static void main(String[] args) throws Exception
	{
		UnicoreExecutor executor = new UnicoreExecutor();

		System.out.println("Load algorithm input data.");
		UnicoreAlgorithmInput algorithmInput = executor.getAlgorithmInput();

		System.out.println("Load dynamic class <" + algorithmInput.getEmbeddedAlgorithm() + ">.");
		Class<?> algorithmClass = UnicoreExecutor.class.getClassLoader().loadClass(algorithmInput.getEmbeddedAlgorithm());

		System.out.println("Create new instance of algorithm.");
		Object algorithmObject = algorithmClass.newInstance();

		System.out.println("Get run method.");
		Method algorithmMethod = algorithmClass.getMethod(METHOD_NAME, METHOD_PARAMETER);

		System.out.println("Create input data structure.");
		Object[] algorithmArguments = {
			algorithmInput.getData()
		};

		System.out.println("Invoke run method.");
		Map data = (Map) algorithmMethod.invoke(algorithmObject, algorithmArguments);

		System.out.println("Invoke run method.");
		UnicoreAlgorithmOutput algorithmOutput = new UnicoreAlgorithmOutput(data);

		System.out.println("Write job output data.");
		executor.putAlgorithmOutput(algorithmOutput);
	}

	protected UnicoreAlgorithmInput getAlgorithmInput() throws ExceptionReport
	{
		try
		{
			FileInputStream fis = new FileInputStream(userDirectory + "/" + UnicoreTask.TARGET_SYSTEM_INPUT_FILE_NAME);
			byte[] data = Compression
					.createUncompressedData(fis, Boolean.parseBoolean(unicoreProperties.getProperty(UnicoreAlgorithmRepository.CFG_COMPRESSION)));
			ByteArrayInputStream bais = new ByteArrayInputStream(data);
			ObjectInputStream ois = new ObjectInputStream(bais);
			UnicoreAlgorithmInput algorithmInput = (UnicoreAlgorithmInput) ois.readObject();
			return algorithmInput;
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
			System.out.println("Error while accessing serialized algorithm input data file.");
			throw new ExceptionReport("Error while accessing serialized algorithm input data file.", ExceptionReport.REMOTE_COMPUTATION_ERROR, e);
		}
		catch (IOException e)
		{
			e.printStackTrace();
			System.out.println("Error while accessing serialized algorithm input data file.");
			throw new ExceptionReport("Error while accessing serialized algorithm input data file.", ExceptionReport.REMOTE_COMPUTATION_ERROR, e);
		}
		catch (ClassNotFoundException e)
		{
			e.printStackTrace();
			System.out.println("Error while accessing serialized algorithm input data file.");
			throw new ExceptionReport("Error while accessing serialized algorithm input data file.", ExceptionReport.REMOTE_COMPUTATION_ERROR, e);
		}
	}

	protected void putAlgorithmOutput(UnicoreAlgorithmOutput pAlgorithmOutput) throws FileNotFoundException, IOException
	{
		byte[] data = Compression.toByteArray(pAlgorithmOutput);
		byte[] dataCompressed = Compression.createCompressedData(data, Boolean
				.parseBoolean(unicoreProperties.getProperty(UnicoreAlgorithmRepository.CFG_COMPRESSION)));
		FileOutputStream fos = new FileOutputStream(userDirectory + "/" + UnicoreTask.TARGET_SYSTEM_OUTPUT_FILE_NAME);
		fos.write(dataCompressed);
		fos.close();
	}
}
