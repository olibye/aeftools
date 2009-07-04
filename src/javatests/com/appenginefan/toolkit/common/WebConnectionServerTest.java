package com.appenginefan.toolkit.common;

import static com.appenginefan.toolkit.common.WebConnectionClientTest.EMPTY;
import static com.appenginefan.toolkit.common.WebConnectionClientTest.META_A;
import static com.appenginefan.toolkit.common.WebConnectionClientTest.p;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.json.JSONObject;

import com.appenginefan.toolkit.persistence.Persistence;
import com.google.common.base.Function;

/**
 * Unit tests for the non-abstract parts of WebConnectionServer
 * 
 * @author Jens Scheffler
 *
 */
public class WebConnectionServerTest extends TestCase { //implements Comparator {
  
  private static String KEY = "key";
  
  private IMocksControl control;
  
  private WebConnectionServer.ServerGuts marshaller;
  private WebConnectionServer server;
  private WebConnectionServer.Receiver receiver;
  
  private ServerEndpoint endpoint;
  private Function<String, String> onOpen;
  
  private HttpServletRequest req; 
  private HttpServletResponse resp;
  private BufferedReader reader;
  private String requestBody;
  
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    control = EasyMock.createStrictControl();
    req = control.createMock(HttpServletRequest.class);    
    resp = control.createMock(HttpServletResponse.class);
    marshaller = control.createMock(WebConnectionServer.ServerGuts.class);
    receiver = control.createMock(WebConnectionServer.Receiver.class);
    onOpen = control.createMock(Function.class);
    endpoint = new ServerEndpoint(KEY, control.createMock(Persistence.class)) {
      @Override
      public void open() {
        onOpen.apply(KEY);
      }
    };
    reader = new BufferedReader(new Reader(){
      
      private StringReader reader;

      @Override
      public void close() throws IOException {
        reader.close();
      }

      @Override
      public int read(char[] arg0, int arg1, int arg2)
          throws IOException {
        if (reader == null) {
          reader = new StringReader(requestBody);
        }
        return reader.read(arg0, arg1, arg2);
      }});
    EasyMock.expect(req.getReader()).andReturn(reader);
    server = new WebConnectionServer(marshaller);
  }
  
  public void testUnparseableJSON() throws IOException {
    requestBody = "foo";
    control.replay();
    assertFalse(server.dispatch(receiver, req, resp));
    control.verify();
  }

  public void testValidXmlButWrongStructure() throws IOException {
    requestBody = new JSONObject().toString();
    control.replay();
    assertFalse(server.dispatch(receiver, req, resp));
    control.verify();
  }
  
  public void testNewConnection() throws IOException {
    requestBody = EMPTY;
    EasyMock.expect(marshaller.newServerEndpoint(req)).andReturn(endpoint);
    EasyMock.expect(onOpen.apply(KEY)).andReturn(KEY);
    marshaller.writeState(req, endpoint, resp);
    marshaller.commit(req);
    control.replay();
    assertTrue(server.dispatch(receiver, req, resp));
    control.verify();
  }
  

  public void testExistingConnection() throws IOException {
    requestBody = META_A;
    EasyMock.expect(marshaller.loadServerEndpoint(req, "a")).andReturn(endpoint);
    marshaller.writeState(req, endpoint, resp);
    marshaller.commit(req);
    control.replay();
    assertTrue(server.dispatch(receiver, req, resp));
    control.verify();
  }
  
  public void testParseContent() throws Exception {
    requestBody = p("foo", "a", "b");
    EasyMock.expect(marshaller.loadServerEndpoint(req, "foo")).andReturn(endpoint);
    receiver.receive(server, endpoint, "a", req);
    receiver.receive(server, endpoint, "b", req);
    marshaller.writeState(req, endpoint, resp);
    marshaller.commit(req);
    control.replay();
    assertTrue(server.dispatch(receiver, req, resp));
    control.verify();
  }
  
  public void testExceptionInReceiver() throws Exception {
    requestBody = p("foo", "a", "b", "c");
    EasyMock.expect(marshaller.loadServerEndpoint(req, "foo")).andReturn(endpoint);
    receiver.receive(server, endpoint, "a", req);
    receiver.receive(server, endpoint, "b", req);
    EasyMock.expectLastCall().andThrow(new RuntimeException("error in receiver"));
    receiver.receive(server, endpoint, "c", req);
    marshaller.writeState(req, endpoint, resp);
    marshaller.commit(req);
    control.replay();
    assertTrue(server.dispatch(receiver, req, resp));
    control.verify();
  }
  
  public void testRollback() throws Exception {
    requestBody = META_A;
    EasyMock.expect(marshaller.loadServerEndpoint(req, "a")).andReturn(endpoint);
    marshaller.writeState(req, endpoint, resp);
    RuntimeException ex = new RuntimeException("expected");
    EasyMock.expectLastCall().andThrow(ex);
    marshaller.rollback(req);
    control.replay();
    try {
      server.dispatch(receiver, req, resp);
      fail("Should have thrown a RuntimeException");
    } catch (RuntimeException e) {
      assertEquals(e, ex);
    }
    control.verify();
  }
}
