package moa.gui;

import org.assertj.swing.core.GenericTypeMatcher;
import org.assertj.swing.dependency.jsr305.Nonnull;
import org.assertj.swing.edt.FailOnThreadViolationRepaintManager;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.junit.testcase.AssertJSwingJUnitTestCase;
import org.junit.Test;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.Objects;

import static org.assertj.swing.finder.WindowFinder.findFrame;
import static org.assertj.swing.launcher.ApplicationLauncher.application;
import static org.junit.Assert.*;


public class SubspaceGuiTest extends AssertJSwingJUnitTestCase{

    private FrameFixture frame;
    @Override
    protected void onSetUp() {
        application(SubspaceGui.class).start();
        this.frame = findFrame(new GenericTypeMatcher<Frame>(Frame.class) {
            protected boolean isMatching(Frame frame) {
                return "Subspacemoa Graphical User Interface".equals(frame.getTitle()) && frame.isShowing();
            }
        }).using(robot());
        FailOnThreadViolationRepaintManager.uninstall();
    }
    @org.junit.Test
    public void shouldBeAbleToRunForAWhileWithoutExceptions() {
        frame.button(new GenericTypeMatcher<JButton>(JButton.class) {
            private boolean firstReturned = false;
            @Override
            protected boolean isMatching(@Nonnull JButton component) {
                if(firstReturned)  return false;
                else {
                    boolean res = component.getText().equals("Edit") && component.isShowing();
                    if(res) this.firstReturned = true;
                    return res;
                }
            }
        }).click();

        frame.dialog().comboBox().selectItem(0);
        frame.dialog().button(new GenericTypeMatcher<JButton>(JButton.class) {
            @Override
            protected boolean isMatching(@Nonnull JButton component) {
                return Objects.equals(component.getText(), "OK");
            }
        }).click();
        frame.button(new GenericTypeMatcher<JButton>(JButton.class) {
            @Override
            protected boolean isMatching(@Nonnull JButton component) {
                return Objects.equals(component.getText(), "Start") && component.isShowing();
            }
        }).click();

    }
}
