## 集成spring
本框架的backserver中有一个BeanContainer接口，该接口有两个实现，一个是BaseBeanContainer，该BeanContainer是一个独立的简单实现，用于没有集成spring的项目，对于集成过spring的项目，可以使用SpringBeanContainer，使用示例如下：
```java
import com.joe.easysocket.server.backserver.mvc.impl.container.CustomClassPathBeanDefinitionScanner;
import com.joe.easysocket.server.backserver.mvc.impl.container.SpringBeanContainer;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * 集成spring，使用SpringBeanContainer的示例
 *
 * @author joe
 * @version 2018.06.25 18:13
 */
public class SpringTest {

    public static void main(String[] args) throws Exception {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        //扫描添加自定义注解的bean
        CustomClassPathBeanDefinitionScanner scanner = new CustomClassPathBeanDefinitionScanner(context);
        scanner.scan("com");
        context.refresh();
        new Thread(() -> Starter.startBackserver(new SpringBeanContainer(context)), "backserver").start();
        new Thread(Starter::startBalance, "balance").start();
    }
}
```
可以看到上边有用到CustomClassPathBeanDefinitionScanner，该类的作用主要是用于让spring发现自定义注解的bean，如果不想使用这种方法，可以将下面几行注释，然后在添加自定义注解的bean上额外添加spring的注解例如@Component等，或者使用xml的可以自己在xml中定义本框架需要的bean（使用@Provider和@Path注解的bean，详情参照[README](../README.md)），但是这样做会侵入代码，造成一定的改造成本（如果原来没有集成spring现在需要集成或者现在集成了spring但是后续可能需要去掉spring），所以建议使用上述示例的方法。
```
//扫描添加自定义注解的bean
CustomClassPathBeanDefinitionScanner scanner = new CustomClassPathBeanDefinitionScanner(context);
scanner.scan("com");
```

[下一节：UDP支持](udp-supported.md)