package fr.inria.yajta;


import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;


public class Agent {
    static String[] INCLUDES = new String[] {
    };
    static String[] ISOTOPES = new String[] {
    };

    static Tracking trackingInstance;

    public static Tracking getTrackingInstance() {
        return trackingInstance;
    }

    public static void premain(String agentArgs, Instrumentation inst) {
        System.err.println("[Premain] Begin '" + agentArgs + "'");
        Args a = new Args();
        a.parseArgs(agentArgs);

        if(a.follow != null) {
            Follower f = new Follower();
            f.load(a.follow);
            trackingInstance = f;
        } else {
            Logger l =  new Logger();
            if(a.output != null)
                l.log = a.output;
            trackingInstance = l;
        }

        final Tracer transformer = new Tracer(format(a.INCLUDES),format(a.EXCLUDES),format(a.ISOTOPES));

        if(a.strictIncludes) transformer.strictIncludes = true;
        if(!a.printTree) transformer.printTree = false;
        INCLUDES = a.INCLUDES;
        ISOTOPES = a.ISOTOPES;
        inst.addTransformer(transformer, true);
        if(inst.isNativeMethodPrefixSupported()) {
            inst.setNativeMethodPrefix(transformer,"wrapped_native_method_");
        }

        Class cl[] = inst.getAllLoadedClasses();

        //System.err.println("isRedefineClassesSupported: " + inst.isRedefineClassesSupported());

        for(int i = cl.length-1; i >= 0; i--) {
            for( String include : INCLUDES ) {

                if (cl[i].getName().startsWith(include)) {
                    try {
                        //System.err.println(cl[i].getName());
                        inst.retransformClasses(cl[i]);

                    } catch (UnmodifiableClassException e) {
                        System.err.println("err: " + cl[i].getName());
                    }
                } else {

                }
            }
            for( String isotope : ISOTOPES ) {
                if (cl[i].getName().startsWith(isotope)) {
                    try {
                        inst.retransformClasses(cl[i]);
                    } catch (UnmodifiableClassException e) {
                        System.err.println("err: " + cl[i].getName());
                    }
                }
            }
        }

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                getTrackingInstance().flush();
            }
        });
        System.err.println("[Premain] Done");


    }

    public static String[] format(String[] ar) {
        String[] res = new String[ar.length];
        for(int i = 0; i < ar.length; i++) res[i] = ar[i].replace(".","/");
        return res;
    }



}
