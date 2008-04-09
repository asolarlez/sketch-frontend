
package streamit.frontend;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class CommandLineParamManager{
	public static class POpts{
		/**
		 * A flag is of the form --flag with no extra parameters.
		 */
		static final int FLAG = 0;
		/**
		 * A number is a flag of the form --flag n where n is a number.
		 */
		static final int NUMBER = 1;

		static final int TOKEN = 2;
		/**
		 * A string is a flag of the form --flag s where s is a string.
		 */
		static final int STRING = 3;
		static final int MULTISTRING = 5;
		static final int VVAL = 4;
		Map<String, String> tokenDescriptions;

		String description;
		String defVal;
		int type;

		POpts(int type, String descrip, String defVal, Map<String, String> td ){
			this.description = descrip;
			this.defVal = defVal;
			this.type = type;
			this.tokenDescriptions = td;
		}
		public String toString(){
			switch(type){
				case STRING:
				case MULTISTRING:
				case FLAG:
				case NUMBER:{
					String msg = description;
					if(defVal != null ){
						msg += "\n \t\t Default value is " + defVal;
					}
					return msg;
				}
				case TOKEN:{
					String msg = description;
					msg += "\n\tOPT can be:";

					for(Iterator<Entry<String, String> > it = tokenDescriptions.entrySet().iterator(); it.hasNext();  ){
						Entry<String, String> en = it.next();
						msg += "\n\t " + en.getKey() + " : \t" + en.getValue();
					}
					return msg;
				}
				case VVAL:{
					return description;
				}
				default:
					return null;
			}
		}

	}
	Map<String, POpts> allowedParameters;
	Map<String, Object> passedParameters;
	List<String> inputFiles = new ArrayList<String>();
	public List<String> backendOptions = new ArrayList<String>();

	CommandLineParamManager(){
		allowedParameters = new HashMap<String, POpts>();
		passedParameters = new HashMap<String, Object>();
	}


	public void loadParams(String[] args){
		for(int i=0; i<args.length; ){
			if( args[i].charAt(0)=='-') {
				if( args[i].charAt(1)=='-' ){

					i+= readParameter(args[i], i+1< args.length ? args[i+1] : "", i+2< args.length ?args[i+2]:"");

				}else{
					backendOptions.add(args[i]);
					if( args[i+1].charAt(0) != '-' ){
						//System.out.println("BACKEND FLAG " + args[i] + " " + args[i+1]);
						backendOptions.add(args[i+1]);
						i+= 2;
					}else{
						//System.out.println("BACKEND FLAG " + args[i] );
						i+= 1;
					}
				}
			} else {
				inputFiles.add(args[i]);
				i+= 1;
			}
		}

		if( !(inputFiles.size() > 0)){
			System.err.println("You did not specify any input files!!");
			printHelp();
			System.exit(1);
		}
	}

	public void setAllowedParam(String flag, POpts po){
		allowedParameters.put(flag, po);
	}


	public void printHelp(){
		for(Iterator<Entry<String, POpts> >  it = allowedParameters.entrySet().iterator(); it.hasNext();   ){
			Entry<String, POpts> en = it.next();
			System.out.println(en.getValue());
		}
	}

	/***
	 *
	 * @param argn
	 * @param argnp1
	 * @return returns the number of arguments consumed;
	 */
	@SuppressWarnings("unchecked")
	private int readParameter(String argn, String argnp1, String argnp2){
		if(argn.equals("--help")){ printHelp(); System.exit(1);  return 1; }

		assert argn.charAt(0) == '-' && argn.charAt(1) == '-' : "Something is wrong here.";
		argn = argn.substring(2);

		if( allowedParameters.containsKey(argn) ){
			POpts argInfo = allowedParameters.get(argn);
			switch( argInfo.type ){
				case POpts.FLAG:{
					passedParameters.put(argn, "TRUE");
					return 1;
				}
				case POpts.NUMBER:
				case POpts.STRING:
				{
					if(argnp1.length() < 1){ throw new RuntimeException("Flag " + argn + " requires an additional argument. \n" + argInfo); }
					passedParameters.put(argn, argnp1);
					return 2;
				}
				case POpts.MULTISTRING:{
					if(passedParameters.containsKey(argn)){
						List<String> ls = (List<String>)passedParameters.get(argn);
						ls.add(argnp1);
					}else{
						List<String> ls = new ArrayList<String>();
						passedParameters.put(argn, ls);
						ls.add(argnp1);
					}
				}
				case POpts.TOKEN:{
					if(argnp1.length() < 1){ throw new RuntimeException("Flag " + argn + " requires an additional argument. \n" + argInfo); }
					if( !argInfo.tokenDescriptions.containsKey(argnp1) ){
						throw new RuntimeException("The argument " + argnp1 + " is not allowed for flag " + argn + ". \n" + argInfo);
					}
					passedParameters.put(argn, argnp1);
					return 2;
				}

				case POpts.VVAL:{
					if(argnp1.length() < 1){ throw new RuntimeException("Flag " + argn + " requires two additional arguments. \n" + argInfo); }
					if(argnp2.length() < 1){ throw new RuntimeException("Flag " + argn + " requires two additional arguments. \n" + argInfo); }
					if( !passedParameters.containsKey(argn) ){
						passedParameters.put(argn, new HashMap<String, String>());
					}
					((Map) passedParameters.get(argn)).put(argnp1, argnp2);
				}

				default:
					throw new RuntimeException(" There was an error with argument " + argn + ". Report this as a bug to the SKETCH team.");
			}
		}else{
			throw new RuntimeException(" The command line argument " + argn + " is not recognized!!");
		}
	}



	protected void checkFlagAllowed(String flag){
		if( !allowedParameters.containsKey(flag) ){
			throw new RuntimeException("The flag " + flag + " does not exist.");
		}
	}

	public boolean hasFlag(String flag){
		checkFlagAllowed(flag);
		return passedParameters.containsKey(flag);
	}


	public List<String> listValue(String flag){
		checkFlagAllowed(flag);
		List<String> val = null;
		if(passedParameters.containsKey(flag)){
			val = (List)passedParameters.get(flag);
		}
		return val;
	}


	/**
	 * Returns the string value of a given flag.
	 * If the flag was not passed, produces its default value.
	 * Includes an implicit check to see if the flag is valid.
	 * @param flag
	 * @return
	 */
	public String sValue(String flag){
		checkFlagAllowed(flag);
		String val = null;
		if(passedParameters.containsKey(flag)){
			val = (String)passedParameters.get(flag);
		}else{

			val = allowedParameters.get(flag).defVal;
		}
		return val;
	}

	/**
	 * Returns the integer value of a given flag.
	 * If the flag was not passed, produces its default value.
	 * Includes an implicit check to see if the flag is valid.
	 * @param flag
	 * @return
	 */
	public int flagValue(String flag){
		String val = sValue(flag);
		Integer i = Integer.decode(val);
		return i;
	}

	public Integer varValue(String flag, String var){
		checkFlagAllowed(flag);
		if( passedParameters.containsKey(flag) ){
			Map<String, String> map = ((Map<String, String>) passedParameters.get(flag));
			if(map.containsKey(var)){
				String val = map.get(var);
				return  Integer.decode(val);
			}
		}
		return null;
	}

	public Map<String, Integer> varValues(String flag){
		checkFlagAllowed(flag);
		if( passedParameters.containsKey(flag) ){
			Map<String, String> map = ((Map<String, String>) passedParameters.get(flag));
			Map<String, Integer> outMap = new HashMap<String, Integer>();
			for(Iterator<Entry<String, String>> it = map.entrySet().iterator(); it.hasNext(); ){
				Entry<String, String> es  = it.next();
				outMap.put(es.getKey(), Integer.decode(es.getValue()));
			}
			return outMap;
		}
		return null;
	}

	public boolean flagEquals(String flag, String candidate){
		checkFlagAllowed(flag);
		String val = null;
		if(passedParameters.containsKey(flag)){
			val = (String)passedParameters.get(flag);
		}else{

			val = allowedParameters.get(flag).defVal;
		}
		return val.equals(candidate);
	}

}