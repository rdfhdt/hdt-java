package org.rdfhdt.hdt.options;

/**
 * keys usable with {@link org.rdfhdt.hdt.options.HDTOptions#set(String, String)}
 * @author Antoine Willerval
 */
public class HDTOptionsKeys {
	/**
	 * Key for the compression mode for the {@link org.rdfhdt.hdt.hdt.HDTManager} generateHDTDisk methods.
	 * Value can be {@link #LOADER_DISK_COMPRESSION_MODE_VALUE_COMPLETE} or
	 * {@link #LOADER_DISK_COMPRESSION_MODE_VALUE_COMPLETE}
	 */
	public static final String LOADER_DISK_COMPRESSION_MODE_KEY = "loader.disk.compressMode";
	/**
	 * Value for {@link #LOADER_DISK_COMPRESSION_MODE_KEY}, sort all the file before going to the next step, slower
	 * but decrease the RAM usage. default config.
	 */
	public static final String LOADER_DISK_COMPRESSION_MODE_VALUE_COMPLETE = "compressionComplete";
	/**
	 * Value for {@link #LOADER_DISK_COMPRESSION_MODE_KEY}, sort while reading all the file before going to the next
	 * step, faster but increase the RAM usage.
	 */
	public static final String LOADER_DISK_COMPRESSION_MODE_VALUE_PARTIAL = "compressionPartial";
	/**
	 * Key for the {@link org.rdfhdt.hdt.hdt.HDTManager} generateHDTDisk methods,
	 * say the number of workers to merge the data. default to the number of processor. long value.
	 */
	public static final String LOADER_DISK_COMPRESSION_WORKER_KEY = "loader.disk.compressWorker";
	/**
	 * Key for the maximum size of a chunk on disk for the {@link org.rdfhdt.hdt.hdt.HDTManager} generateHDTDisk
	 * methods, the chunk should be in RAM before writing it on disk and should be sorted. long value.
	 */
	public static final String LOADER_DISK_CHUNK_SIZE_KEY = "loader.disk.chunkSize";
	/**
	 * Key for the location of the working directory {@link org.rdfhdt.hdt.hdt.HDTManager} generateHDTDisk methods,
	 * this directory will be deleted after the HDT generation. by default, the value is random, it is recommended to
	 * set this option to delete the directory in case of an interruption of the process. file value.
	 */
	public static final String LOADER_DISK_LOCATION_KEY = "loader.disk.location";
	/**
	 * Key for the location of the future HDT for the {@link org.rdfhdt.hdt.hdt.HDTManager} generateHDTDisk methods,
	 * this option will create a hdt file after the HDT generation, the returned HDT will be a mapped HDT of the HDT
	 * file. slower, increase the disk usage, but drastically reduce the RAM usage. file value.
	 */
	public static final String LOADER_DISK_FUTURE_HDT_LOCATION_KEY = "loader.disk.futureHDTLocation";
	/**
	 * Key for the maximum number of file opened at the same time, should be greater than {@link #LOADER_DISK_KWAY_KEY},
	 * 1024 by default
	 */
	public static final String LOADER_DISK_MAX_FILE_OPEN_KEY = "loader.disk.maxFileOpen";
	/**
	 * Key for the number of chunk layers opened at the same time, by default
	 * <p>min(log2(maxFileOpen), chunkSize / (fileBufferSize * compressWorker))</p>
	 */
	public static final String LOADER_DISK_KWAY_KEY = "loader.disk.kway";
	/**
	 * Key for the size of the buffers when opening a file
	 */
	public static final String LOADER_DISK_BUFFER_SIZE_KEY = "loader.disk.fileBufferSize";
	/**
	 * Key for the loading mode of a RDF file for the
	 * {@link org.rdfhdt.hdt.hdt.HDTManager#generateHDT(String, String, org.rdfhdt.hdt.enums.RDFNotation, HDTOptions, org.rdfhdt.hdt.listener.ProgressListener)}
	 * method, this key isn't working with the other methods.
	 * Value can be {@link #LOADER_TYPE_VALUE_ONE_PASS} or {@link #LOADER_TYPE_VALUE_TWO_PASS}.
	 */
	public static final String LOADER_TYPE_KEY = "loader.type";
	/**
	 * Value for {@link #LOADER_TYPE_KEY}, read twice the RDF file, reduce the RAM usage
	 */
	public static final String LOADER_TYPE_VALUE_TWO_PASS = "two-pass";
	/**
	 * Value for {@link #LOADER_TYPE_KEY}, read only once the RDF file, default value
	 */
	public static final String LOADER_TYPE_VALUE_ONE_PASS = "one-pass";

	private HDTOptionsKeys() {}
}
