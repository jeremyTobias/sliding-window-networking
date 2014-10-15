import java.util.concurrent.*;

public class PageReader extends Thread {
	private ThreadPoolExecutor threadPool = null;
	private ThreadPoolExecutor priorityThreadPool = null;
	
	private LinkedBlockingQueue<PageRequestHandler> queue;
	private LinkedBlockingQueue<PageRequestHandler> priorityQueue;
	private LinkedBlockingQueue<RrqPacket> rrqPacket;
	private LinkedBlockingQueue<RrqPacket> priorityRrqPacket;
	
	private boolean useSliding;
	private boolean useDrop;

	private int pool = 30;
	private int maxPool = 2000;
	private long timeToLive = 10500;

	public PageReader(boolean useSliding, boolean useDrop) {
		this.useSliding = useSliding;
		this.useDrop = useDrop;
		this.queueInit();
		this.exectutorInit();
	}

	private void queueInit() {
		queue = new LinkedBlockingQueue<PageRequestHandler>();
		priorityQueue = new LinkedBlockingQueue<PageRequestHandler>();
		rrqPacket = new LinkedBlockingQueue<RrqPacket>();
		priorityRrqPacket = new LinkedBlockingQueue<RrqPacket>();
	}

	@SuppressWarnings("unchecked")
	private void exectutorInit() {
		threadPool = new ThreadPoolExecutor(pool, maxPool, timeToLive, TimeUnit.MILLISECONDS,
			(BlockingQueue)getQueue());
		priorityThreadPool = new ThreadPoolExecutor(4, 6, timeToLive, TimeUnit.MILLISECONDS,
			(BlockingQueue)getPriorityQueue());
	}

	public void addToQueue(RrqPacket rrqPacket) {
		try {
			getPackets().put(rrqPacket);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void addToPriorityQueue(RrqPacket rrqPacket) {
		try {
			getPriorityPackets().put(rrqPacket);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void run() {
		for (;;) {
			try {
				if (!getPriorityPackets().isEmpty()) {
					priorityThreadPool.submit(new PageRequestHandler(getPriorityPackets().take(), this.useSliding, this.useDrop));
					continue;
				} 
			} catch (RejectedExecutionException ree) {
					ree.printStackTrace();
			} catch (Exception e) {
					e.printStackTrace();
			}

			try {
				threadPool.submit(new PageRequestHandler(getPackets().take(), this.useSliding, this.useDrop));
			} catch (RejectedExecutionException ree) {
				ree.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void shutdown() {
		threadPool.shutdown();
	}

	public LinkedBlockingQueue<PageRequestHandler> getQueue() {
		return queue;
	}

	public LinkedBlockingQueue<PageRequestHandler> getPriorityQueue() {
		return priorityQueue;
	}

	public LinkedBlockingQueue<RrqPacket> getPackets() {
		return rrqPacket;
	}

	public LinkedBlockingQueue<RrqPacket> getPriorityPackets() {
		return priorityRrqPacket;
	}
}