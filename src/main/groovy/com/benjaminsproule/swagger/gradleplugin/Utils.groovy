package com.benjaminsproule.swagger.gradleplugin

import com.benjaminsproule.swagger.gradleplugin.except.GenerateException
import io.swagger.models.ArrayModel
import io.swagger.models.ComposedModel
import io.swagger.models.Model
import io.swagger.models.ModelImpl
import io.swagger.models.Operation
import io.swagger.models.Path
import io.swagger.models.RefModel
import io.swagger.models.Response
import io.swagger.models.Swagger
import io.swagger.models.Tag
import io.swagger.models.parameters.Parameter
import io.swagger.models.properties.Property
import org.springframework.core.annotation.AnnotationUtils
import org.springframework.web.bind.annotation.RequestMapping

import java.lang.reflect.Field
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

class Utils {
    public static final String CLASSPATH = "classpath:"

    private static final HTTP_METHODS = ["Get", "Delete", "Post", "Put", "Options", "Patch"]

    /**
     * Extracts all routes from the annotated class
     *
     * @param controllerClazz
     *            Instrospected class
     * @return At least 1 route value (empty string)
     */
    static String[] getControllerRequestMapping(Class<?> controllerClazz) {
        String[] controllerRequestMappingValues = {}

        // Determine if we will use class-level requestmapping or dummy string
        RequestMapping classRequestMapping = AnnotationUtils.findAnnotation(controllerClazz, RequestMapping.class)
        if (classRequestMapping != null) {
            controllerRequestMappingValues = classRequestMapping.value()
        }

        if (controllerRequestMappingValues.length == 0) {
            controllerRequestMappingValues = new String[1]
            controllerRequestMappingValues[0] = ""
        }
        return controllerRequestMappingValues
    }

    static void sortSwagger(Swagger swagger) throws GenerateException {
        if (swagger == null || swagger.getPaths() == null) {
            return
        }

        TreeMap<String, Path> sortedMap = new TreeMap<String, Path>()
        sortedMap.putAll(swagger.getPaths())
        swagger.paths(sortedMap)

        for (Path path : swagger.getPaths().values()) {
            for (String m : HTTP_METHODS) {
                sortResponses(path, m)
            }
        }

        //reorder definitions
        if (swagger.getDefinitions() != null) {
            TreeMap<String, Model> defs = new TreeMap<String, Model>()
            for ( Model model: swagger.getDefinitions().values()) {
                sortModel(model);
            }
            defs.putAll(swagger.getDefinitions())
            swagger.setDefinitions(defs)
        }

        // order the tags
        if (swagger.getTags() != null) {
            Collections.sort(swagger.getTags(), new Comparator<Tag>() {
                int compare(final Tag a, final Tag b) {
                    return a.toString().compareTo(b.toString())
                }
            })
        }

    }

    private static void sortModel(Model model) {
        if (model instanceof ComposedModel) {
            ComposedModel cm = (ComposedModel)model;
            for(Model childModel : cm.getAllOf()) {
                sortModel(childModel);
            }
        } else {
            sortProperties(model, model.getProperties());
        }
    }

    private static void sortProperties(Model model, Map<String, Property> properties) {
        if (properties != null) {
            TreeMap<String> sortedProps = new TreeMap<String>(new Comparator<String>() {
                @Override
                int compare(String o1, String o2) {
                    return o1.compareTo(o2);
                }
            });
            sortedProps.putAll(properties);
            Field field = model.getClass().getDeclaredField("properties");
            field.setAccessible(true);
            field.set(model, sortedProps);
        }
    }

    private static void sortResponses(Path path, String method) throws GenerateException {
        try {
            Method m = Path.class.getDeclaredMethod("get" + method)
            Operation op = (Operation) m.invoke(path)
            if (op == null) {
                return
            }
            Map<String, Response> responses = op.getResponses()
            TreeMap<String, Response> res = new TreeMap<String, Response>()
            res.putAll(responses)
            op.getParameters().sort(new Comparator<Parameter>() {
                @Override
                int compare(Parameter o1, Parameter o2) {
                    return o1.getName().compareTo(o2.getName())
                }
            })
            op.setResponses(res)
        } catch (NoSuchMethodException e) {
            throw new GenerateException(e)
        } catch (InvocationTargetException e) {
            throw new GenerateException(e)
        } catch (IllegalAccessException e) {
            throw new GenerateException(e)
        }
    }
}
