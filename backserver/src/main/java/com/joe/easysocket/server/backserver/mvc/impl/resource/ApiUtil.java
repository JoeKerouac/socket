package com.joe.easysocket.server.backserver.mvc.impl.resource;

import com.joe.easysocket.server.backserver.mvc.impl.exception.ParamterNoNamingException;
import com.joe.easysocket.server.backserver.mvc.impl.param.Context;
import com.joe.easysocket.server.backserver.mvc.impl.param.GeneralParam;
import com.joe.easysocket.server.backserver.mvc.impl.resource.annotation.Consumes;
import com.joe.easysocket.server.backserver.mvc.impl.resource.annotation.Path;
import com.joe.easysocket.server.backserver.mvc.impl.resource.annotation.Produces;
import com.joe.utils.type.JavaType;
import com.joe.utils.type.JavaTypeUtil;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;

/**
 * API构建工具
 *
 * @author joe
 */
class ApiUtil {
    private static GeneralParam defaultGeneralParam = new GeneralParam() {
        @Override
        public Class<? extends Annotation> annotationType() {
            return GeneralParam.class;
        }

        @Override
        public String value() {
            return "default";
        }
    };

    /**
     * 构建API说明
     *
     * @param instance        resource实例
     * @param resourceClass   resource类
     * @param resourceMethods resource方法集合，该集合里的方法必须都是属于resourceClass的方法
     * @return resource集合
     */
    public static Map<String, Resource> buildResource(Object instance, Class<?> resourceClass,
                                                      Method... resourceMethods) {
        if (resourceMethods == null || resourceMethods.length == 0) {
            return Collections.emptyMap();
        }
        Map<String, Resource> resources = new TreeMap<>();
        for (Method resourceMethod : resourceMethods) {
            Resource resource = buildResource(instance, resourceClass, resourceMethod);
            resources.put(resource.getName(), resource);
        }
        return resources;
    }

    /**
     * 构建resource
     *
     * @param instance       resource实例
     * @param resourceMethod resource方法
     * @param resourceClass  resource类
     * @return resource 根据指定参数构建的资源
     */
    @SuppressWarnings("unchecked")
    public static Resource buildResource(Object instance, Class<?> resourceClass, Method resourceMethod) {
        // 获取API参数
        Parameter[] parameters = resourceMethod.getParameters();
        List<Param> params = new ArrayList<>(parameters.length);
        for (Parameter parameter : parameters) {
            Param param = new Param();
            params.add(param);

            JavaType type = JavaTypeUtil.createJavaType(parameter.getParameterizedType());
            type.setAnnotations(parameter.getAnnotations());
            param.setType(type);

            try {
                param.setName(getParamName(parameter));
            } catch (ParamterNoNamingException e) {
                if (singleton(parameters)) {
                    // 如果只有一个参数，那么允许不命名
                    param.setName("default");
                    Annotation[] old = type.getAnnotations();
                    Annotation[] now = new Annotation[old.length + 1];
                    System.arraycopy(old, 0, now, 0, old.length);
                    now[old.length] = defaultGeneralParam;
                    type.setAnnotations(now);
                } else {
                    throw e;
                }
            }
        }

        resourceMethod.setAccessible(true);

        Resource resource = new Resource();
        resource.setName(getURI(resourceMethod, resourceClass));
        resource.setConsume(getConsume(resourceMethod));
        resource.setProduce(getProduce(resourceMethod));
        resource.setParams(params);
        resource.setResourceClass(resourceClass);
        resource.setResourceMethod(resourceMethod);
        resource.setInstance(instance);
        return resource;
    }

    /**
     * 获取资源的路径
     *
     * @param resourceMethod 接口方法
     * @param resourceClass  接口类
     * @return 该资源的路径
     */
    private static String getURI(Method resourceMethod, Class<?> resourceClass) {
        String[] pathStrs = new String[2];
        // 获取接口路径
        if (resourceClass.isAnnotationPresent(Path.class)) {
            Path path = resourceClass.getAnnotation(Path.class);
            pathStrs[0] = path.value();
        }

        if (resourceMethod.isAnnotationPresent(Path.class)) {
            Path path = resourceMethod.getAnnotation(Path.class);
            pathStrs[1] = path.value();
        }

        StringBuilder sb = new StringBuilder();
        String[] strs;
        sb.append("/");

        for (String pathStr : pathStrs) {
            if (pathStr == null || pathStr.isEmpty()) {
                continue;
            }
            strs = pathStr.split("/");
            for (String str : strs) {
                if (!str.isEmpty()) {
                    sb.append(str).append("/");
                }
            }
        }
        return sb.toString().substring(0, sb.length() - 1);
    }

    /**
     * 获取方法的请求参数的格式
     *
     * @param resourceMethod 指定方法
     * @return 方法的请求参数的格式
     */
    private static String getConsume(Method resourceMethod) {
        Consumes consumes = resourceMethod.getAnnotation(Consumes.class);
        return consumes == null ? "json" : consumes.value();
    }

    /**
     * 获取方法的响应参数的格式
     *
     * @param resourceMethod 指定方法
     * @return 方法的响应参数格式
     */
    private static String getProduce(Method resourceMethod) {
        Produces produces = resourceMethod.getAnnotation(Produces.class);
        return produces == null ? "json" : produces.value();
    }

    /**
     * 获取字段的命名
     *
     * @param parameter 要获取命名的字段
     * @return 指定字段的命名
     */
    private static String getParamName(Parameter parameter) throws ParamterNoNamingException {
        Annotation[] annotations = parameter.getAnnotations();
        GeneralParam generalParam = null;
        Context context = null;
        for (Annotation annotation : annotations) {
            if (annotation instanceof GeneralParam) {
                generalParam = (GeneralParam) annotation;
                break;
            } else if (annotation instanceof Context) {
                context = (Context) annotation;
                break;
            }
        }

        if (generalParam != null && !generalParam.value().trim().isEmpty()) {
            return generalParam.value();
        } else if (context != null) {
            return "Context";
        } else {
            throw new ParamterNoNamingException(parameter);
        }
    }

    /**
     * 判断参数是否是Context参数
     *
     * @param parameter 参数
     * @return 如果返回true说明此参数是Context类型
     */
    private static boolean isContextParam(Parameter parameter) {
        Annotation[] annotations = parameter.getAnnotations();
        for (Annotation annotation : annotations) {
            if (annotation instanceof Context) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断参数集合中除了Context类型的参数外是否只有一个参数
     *
     * @param parameters 参数集合
     * @return 如果参数集合中除了Context类型的参数外只有一个参数那么返回true，否则返回false
     */
    private static boolean singleton(Parameter[] parameters) {
        int flag = 0;
        for (Parameter param : parameters) {
            if (flag > 1) {
                return false;
            }
            if (!isContextParam(param)) {
                flag++;
            }
        }
        return true;
    }
}