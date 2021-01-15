/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.rdfhdt.hdt.fuseki;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

import org.apache.jena.atlas.lib.FileOps;
import org.apache.jena.fuseki.Fuseki;
import org.apache.jena.fuseki.FusekiException;
import org.apache.jena.fuseki.cmd.FusekiArgs;
import org.apache.jena.fuseki.cmd.JettyFusekiWebapp;
import org.apache.jena.fuseki.jetty.JettyServerConfig;
import org.apache.jena.fuseki.mgt.Template;
import org.apache.jena.fuseki.webapp.FusekiEnv;
import org.apache.jena.fuseki.webapp.FusekiServerListener;
import org.apache.jena.fuseki.webapp.FusekiWebapp;
import org.apache.jena.graph.Graph;
import org.apache.jena.query.ARQ;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphFactory;
import org.apache.jena.sys.JenaSystem;
import org.apache.jena.tdb.TDB;
import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.hdt.HDTManager;
import org.rdfhdt.hdtjena.HDTGraph;
import org.slf4j.Logger;

import arq.cmdline.CmdARQ;
import arq.cmdline.ModDatasetAssembler;
import jena.cmd.ArgDecl;
import jena.cmd.CmdException;
import jena.cmd.TerminationException;


/**
 * 
 * Fork of default's FusekiCmd that adds support for loading HDT files.
 *
 */

public class FusekiHDTCmd
{
	static public void main(String... argv) {
        FusekiHDTCmdInner.innerMain(argv);
    }
	static public void innerMain(String... argv)
	{
		JenaSystem.init();
		// Do explicitly so it happens after subsystem initialization.
		Fuseki.init();
		new FusekiHDTCmdInner(argv).mainRun();
	}

	static class FusekiHDTCmdInner
	    extends CmdARQ
	{
		public DatasetGraph dsg;
	
		private static ArgDecl argHDT = new ArgDecl(ArgDecl.HasValue, "hdt");

		// --mgt. --mgtPort  :: Legacy.
		private static ArgDecl argMgt = new ArgDecl(ArgDecl.NoValue, "mgt");
		private static ArgDecl argMgtPort = new ArgDecl(ArgDecl.HasValue, "mgtPort", "mgtport");

		// --home :: Legacy - do not use.
		private static ArgDecl argHome = new ArgDecl(ArgDecl.HasValue, "home");
		// --pages :: Legacy - do not use.
		private static ArgDecl argPages = new ArgDecl(ArgDecl.HasValue, "pages");

		private static ArgDecl argPort = new ArgDecl(ArgDecl.HasValue, "port");
		private static ArgDecl argLocalhost = new ArgDecl(ArgDecl.NoValue, "localhost", "local");
		private static ArgDecl argTimeout = new ArgDecl(ArgDecl.HasValue, "timeout");
		private static ArgDecl argFusekiConfig = new ArgDecl(ArgDecl.HasValue, "config", "conf");
		private static ArgDecl argJettyConfig = new ArgDecl(ArgDecl.HasValue, "jetty-config");
		private static ArgDecl argGZip = new ArgDecl(ArgDecl.HasValue, "gzip");

		// Deprecated.  Use shiro.
		private static ArgDecl argBasicAuth = new ArgDecl(ArgDecl.HasValue, "basic-auth");

		// private static ModLocation modLocation = new ModLocation();
		private static ModDatasetAssembler modDataset = new ModDatasetAssembler();

		static public void innerMain(String... argv)
		{
			JenaSystem.init();
			// Do explicitly so it happens after subsystem initialization.
			Fuseki.init();
			new FusekiHDTCmdInner(argv).mainRun();
		}

		private JettyServerConfig jettyServerConfig = new JettyServerConfig();
		{
			jettyServerConfig.port = 3030;
			jettyServerConfig.contextPath = "/";
			jettyServerConfig.jettyConfigFile = null;
			jettyServerConfig.enableCompression = true;
			jettyServerConfig.verboseLogging = false;
		}

		private final FusekiArgs cmdLine = new FusekiArgs();

		public FusekiHDTCmdInner(String... argv)
		{
			super(argv);

			getUsage().startCategory("Fuseki");
			addModule(modDataset);
			add(argHDT,     "--hdt=HDT",            "Use an existing HDT file") ;
			add(argPort, "--port",
			    "Listen on this port number");
			// Set via jetty config file.
			add(argLocalhost, "--localhost",
			    "Listen only on the localhost interface");
			add(argTimeout, "--timeout=",
			    "Global timeout applied to queries (value in ms) -- format is X[,Y] ");
			add(argFusekiConfig, "--config=",
			    "Use a configuration file to determine the services");
			add(argJettyConfig, "--jetty-config=FILE",
			    "Set up the server (not services) with a Jetty XML file");
			add(argBasicAuth);
			add(argPages);
			add(argMgt); // Legacy
			add(argMgtPort); // Legacy
			add(argGZip, "--gzip=on|off",
			    "Enable GZip compression (HTTP Accept-Encoding) if request header set");

			super.modVersion.addClass(TDB.class);
			super.modVersion.addClass(Fuseki.class);
		}

		static String argUsage = "[--config=FILE] [--mem|--desc=AssemblerFile|--file=FILE] [--port PORT] /DatasetPathName";

		@Override
		protected String getSummary()
		{
			return getCommandName() + " " + argUsage;
		}

		@Override
		protected void processModulesAndArgs()
		{
			if (super.isVerbose() || super.isDebug())
			{
				jettyServerConfig.verboseLogging = true;
				// Output is still at level INFO (currently)
			}
			cmdLine.quiet = super.isQuiet();
			cmdLine.verbose = super.isVerbose();

			// Any final tinkering with FUSEKI_HOME and FUSEKI_BASE, e.g. arguments like --home, --base, then ....
			FusekiEnv.resetEnvironment();

			Logger log = Fuseki.serverLog;

			if (contains(argFusekiConfig))
			{
				cmdLine.fusekiCmdLineConfigFile = getValue(argFusekiConfig);
				cmdLine.datasetDescription = "Configuration: " + cmdLine.fusekiCmdLineConfigFile;
			}
			// ---- Datasets
			// Check one and only way is defined.
			int x = 0;

			if (contains(argHDT))
				x++;

			if (cmdLine.fusekiCmdLineConfigFile != null)
			{
				if (x >= 1)
					throw new CmdException(
					    "Dataset specified on the command line but a configuration file also given.");
			}
			else
			{
				// No configuration file.  0 or 1 legal.
				if (x > 1)
					throw new CmdException(
					    "Multiple ways providing a dataset. Only one of --mem, --file, --loc or --desc");
			}

			boolean cmdlineConfigPresent = (x != 0);
			if (cmdlineConfigPresent && getPositional().size() == 0)
				throw new CmdException("Missing service name");

			if (cmdLine.fusekiCmdLineConfigFile != null && getPositional().size() > 0)
				throw new CmdException("Service name will come from --conf; no command line service name allowed");

			if (!cmdlineConfigPresent && getPositional().size() > 0)
				throw new CmdException(
				    "Service name given but no configuration argument to match (e.g. --mem, --loc/--tdb, --file)");

			if (cmdlineConfigPresent && getPositional().size() > 1)
				throw new CmdException("Multiple dataset path names given" + getPositional().stream().collect(Collectors.joining(", ")));

			if (!cmdlineConfigPresent && cmdLine.fusekiCmdLineConfigFile == null)
			{
				// Turn command line argument into an absolute file name.
				FusekiEnv.setEnvironment();
				Path cfg = FusekiEnv.FUSEKI_BASE.resolve(FusekiWebapp.DFT_CONFIG).toAbsolutePath();
				if (Files.exists(cfg))
					cmdLine.fusekiServerConfigFile = cfg.toString();
			}


			if (contains(argHDT))
			{
				String hdtFile = getValue(argHDT);
				log.info("HDT dataset: file={}", hdtFile);
				if (!FileOps.exists(hdtFile))
					throw new CmdException("HDT file does not exist: " + hdtFile);


				try
				{
					// Single Graph:
					HDT hdt = HDTManager.mapIndexedHDT(hdtFile, null);
					Graph graph = new HDTGraph(hdt);
					dsg = DatasetGraphFactory.wrap(graph);

					// Multiple Graphs:
//						DatasetGraphMap datasetMap = new DatasetGraphMap(defaultGraph);
//						datasetMap.addGraph(graphName, graph);

				} catch (IOException e)
				{
					e.printStackTrace();
					throw new CmdException("Could not load HDT: " + e.getMessage());
				}
			}

			if (cmdlineConfigPresent)
			{
				cmdLine.datasetPath = getPositionalArg(0);
				if (cmdLine.datasetPath.length() > 0 && !cmdLine.datasetPath.startsWith("/"))
					throw new CmdException("Dataset path name must begin with a /: " + cmdLine.datasetPath);
				if (!cmdLine.allowUpdate)
					Fuseki.serverLog.info("Running in read-only mode for " + cmdLine.datasetPath);
				// Include the dataset name as NAME for any templates.
				cmdLine.params.put(Template.NAME, cmdLine.datasetPath);
			}

			// ---- Jetty server
			if (contains(argBasicAuth))
				Fuseki.configLog.warn("--basic-auth ignored: Use Apache Shiro security - see shiro.ini");

			if (contains(argPort))
			{
				String portStr = getValue(argPort);
				try
				{
					jettyServerConfig.port = Integer.parseInt(portStr);
				} catch (NumberFormatException ex)
				{
					throw new CmdException(argPort.getKeyName() + " : bad port number: " + portStr);
				}
			}

			if (contains(argMgt))
				Fuseki.configLog.warn("Fuseki v2: Management functions are always enabled.  --mgt not needed.");

			if (contains(argMgtPort))
				Fuseki.configLog.warn(
				    "Fuseki v2: Management functions are always on the same port as the server.  --mgtPort ignored.");

			if (contains(argLocalhost))
				jettyServerConfig.loopback = true;

			if (contains(argTimeout))
			{
				String str = getValue(argTimeout);
				ARQ.getContext().set(ARQ.queryTimeout, str);
			}

			if (contains(argJettyConfig))
			{
				jettyServerConfig.jettyConfigFile = getValue(argJettyConfig);
				if (!FileOps.exists(jettyServerConfig.jettyConfigFile))
					throw new CmdException("No such file: " + jettyServerConfig.jettyConfigFile);
			}

			if (contains(argBasicAuth))
				Fuseki.configLog.warn("--basic-auth ignored (use Shiro setup instead)");

			if (contains(argHome))
				Fuseki.configLog.warn("--home ignored (use enviroment variables $FUSEKI_HOME and $FUSEKI_BASE)");

			if (contains(argPages))
				Fuseki.configLog.warn("--pages ignored (enviroment variables $FUSEKI_HOME to provide the webapp)");

			if (contains(argGZip))
			{
				if (!hasValueOfTrue(argGZip) && !hasValueOfFalse(argGZip))
					throw new CmdException(argGZip.getNames().get(0) + ": Not understood: " + getValue(argGZip));
				jettyServerConfig.enableCompression = super.hasValueOfTrue(argGZip);
			}
		}

		@Override
		protected void exec()
		{
			try
			{
				runFuseki(cmdLine, jettyServerConfig);
			} catch (FusekiException ex)
			{
				throw new TerminationException(1);
			}
		}

		@Override
		protected String getCommandName()
		{
			return "fuseki";
		}
	}
	
	/** Configure and run a Fuseki server - this function does not return except for error starting up */
    public static void runFuseki(FusekiArgs serverConfig, JettyServerConfig jettyConfig) {
        FusekiServerListener.initialSetup = serverConfig;
        JettyFusekiWebapp.initializeServer(jettyConfig);
        JettyFusekiWebapp.instance.start();
        JettyFusekiWebapp.instance.join();
    }
}
