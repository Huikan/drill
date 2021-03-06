/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.testing;

import org.apache.drill.exec.ops.FragmentContext;
import org.apache.drill.exec.util.AssertionUtil;
import org.slf4j.Logger;

/**
 * Injects exceptions and pauses at execution time for testing. Any class that wants to simulate exceptions
 * or inject pauses for testing should have it's own private static instance of an injector (similar to the use
 * of loggers). Injection site either use {@link org.apache.drill.exec.ops.FragmentContext} or
 * {@link org.apache.drill.exec.ops.QueryContext}. See {@link org.apache.drill.exec.testing.TestExceptionInjection} and
 * {@link org.apache.drill.exec.testing.TestPauseInjection} for examples of use.
 */
public class ExecutionControlsInjector {
//  private static final Logger logger = org.slf4j.LoggerFactory.getLogger(ExecutionControlsInjector.class);

  private final Class<?> clazz; // the class that owns this injector

  /**
   * Constructor. Classes should use the static {@link #getInjector} method to obtain their injector.
   *
   * @param clazz the owning class
   */
  protected ExecutionControlsInjector(final Class<?> clazz) {
    this.clazz = clazz;
  }

  /**
   * Create an injector if assertions are enabled
   *
   * @param clazz the owning class
   * @return the newly created injector
   */
  public static ExecutionControlsInjector getInjector(final Class<?> clazz) {
    if (AssertionUtil.isAssertionsEnabled()) {
      return new ExecutionControlsInjector(clazz);
    } else {
      return new NoOpControlsInjector(clazz);
    }
  }

  /**
   * Get the injector's owning class.
   *
   * @return the injector's owning class
   */
  public Class<?> getSiteClass() {
    return clazz;
  }

  /**
   * Inject (throw) an unchecked exception at this point, if an injection is specified, and it is time
   * for it to be thrown.
   * <p/>
   * <p>Implementors use this in their code at a site where they want to simulate an exception
   * during testing.
   *
   * @param executionControls the controls in the current context
   * @param desc              the site description
   *                          throws the exception specified by the injection, if it is time
   */
  public void injectUnchecked(final ExecutionControls executionControls, final String desc) {
    final ExceptionInjection exceptionInjection = executionControls.lookupExceptionInjection(this, desc);
    if (exceptionInjection != null) {
      exceptionInjection.throwUnchecked();
    }
  }

  /**
   * Inject (throw) an unchecked exception at this point, if the fragmentContext is not null,
   * an injection is specified, and it is time for it to be thrown.
   * <p/>
   * <p>Implementors use this in their code at a site where they want to simulate an exception
   * during testing.
   *
   * @param fragmentContext   fragmentContext used to retrieve the controls, can be null
   * @param desc              the site description
   *                          throws the exception specified by the injection, if it is time
   */
  public void injectUnchecked(final FragmentContext fragmentContext, final String desc) {
    if (fragmentContext != null) {
      injectUnchecked(fragmentContext.getExecutionControls(), desc);
    }
  }

  /**
   * Inject (throw) a checked exception at this point, if an injection is specified, and it is time
   * for it to be thrown.
   * <p/>
   * <p>Implementors use this in their code at a site where they want to simulate an exception
   * during testing.
   *
   * @param executionControls the controls in the current context
   * @param desc              the site description
   * @param exceptionClass    the expected class of the exception (or a super class of it)
   * @throws T the exception specified by the injection, if it is time
   */
  public <T extends Throwable> void injectChecked(
    final ExecutionControls executionControls, final String desc, final Class<T> exceptionClass) throws T {
    final ExceptionInjection exceptionInjection = executionControls.lookupExceptionInjection(this, desc);
    if (exceptionInjection != null) {
      exceptionInjection.throwChecked(exceptionClass);
    }
  }

  /**
   * Pauses at this point, if such an injection is specified (i.e. matches the site description).
   * <p/>
   * <p>Implementors use this in their code at a site where they want to simulate a pause
   * during testing.
   *
   * @param executionControls the controls in the current context
   * @param desc              the site description
   * @param logger            logger of the class containing the injection site
   */
  public void injectPause(final ExecutionControls executionControls, final String desc,
                                               final Logger logger) {
    final PauseInjection pauseInjection =
      executionControls.lookupPauseInjection(this, desc);

    if (pauseInjection != null) {
      logger.debug("Pausing at {}", desc);
      pauseInjection.pause();
      logger.debug("Resuming at {}", desc);
    }
  }

  /**
   * Insert a pause that can be interrupted using {@link Thread#interrupt()} at the given site point, if such an
   * injection is specified (i.e. matches the site description).
   * <p/>
   * <p>Implementors use this in their code at a site where they want to simulate a interruptible pause
   * during testing.
   *
   * @param executionControls the controls in the current context
   * @param desc              the site description
   * @param logger            logger of the class containing the injection site
   * @throws InterruptedException if interrupted using {@link Thread#interrupt()}
   */
  public void injectInterruptiblePause(final ExecutionControls executionControls, final String desc,
      final Logger logger) throws InterruptedException {
    final PauseInjection pauseInjection = executionControls.lookupPauseInjection(this, desc);

    if (pauseInjection != null) {
      logger.debug("Interruptible pausing at {}", desc);
      try {
        pauseInjection.interruptiblePause();
      } catch (final InterruptedException e) {
        logger.debug("Pause interrupted at {}", desc);
        throw e;
      }
      logger.debug("Interruptible pause resuming at {}", desc);
    }
  }

  public CountDownLatchInjection getLatch(final ExecutionControls executionControls, final String desc) {
    return executionControls.lookupCountDownLatchInjection(this, desc);
  }
}
