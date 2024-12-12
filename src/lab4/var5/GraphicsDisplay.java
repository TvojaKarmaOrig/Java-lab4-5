package lab4.var5;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Stroke;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.font.FontRenderContext;
//import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.Console;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Stack;
import javax.swing.JPanel;

@SuppressWarnings("serial")
public class GraphicsDisplay extends JPanel {

    // Список координат точек для построения графика
    private Double[][] graphicsData1;
    private Double[][] graphicsData90;
    private Double[][] graphicsData;

    // Флаговые переменные, задающие правила отображения графика
    private boolean showAxis = true;
    private boolean showMarkers = true;
    private boolean turn90 = false;

    // Границы диапазона пространства, подлежащего отображению
    private double minX;
    private double maxX;
    private double minY;
    private double maxY;

    int height;
    int width;

    private Point2D.Double highlightedPoint;
    private String highlightedCoords;

    private Point2D.Double selectionStart;
    private Point2D.Double selectionEnd;
    private boolean selecting;
    private boolean isTracked = false;

    private int draggedPointIndex; // Индекс перетаскиваемой точки

    // Используемый масштаб отображения
    private double scaleX;
    private double scaleY;

    // Различные стили черчения линий
    private BasicStroke graphicsStroke;
    private BasicStroke axisStroke;
    private BasicStroke markerStroke;

    // Различные шрифты отображения надписей
    private Font axisFont;
    private Font numsFont;

    private Stack<Double[]> memoryStack = new Stack<>();

    public GraphicsDisplay() {
        // Цвет заднего фона области отображения - белый
        setBackground(Color.WHITE);
        this.selectionStart = null;
        this.selectionEnd = null;
        this.selecting = false;
        // Сконструировать необходимые объекты, используемые в рисовании
        // Перо для рисования графика
        float[] dashPattern = {16, 4, 4, 4, 8, 4, 4, 4};
        graphicsStroke = new BasicStroke(3, BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_ROUND, 10.0f, dashPattern, 0.0f);
        // Перо для рисования осей координат
        axisStroke = new BasicStroke(2.0f, BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_MITER, 10.0f, null, 0.0f);
        // Перо для рисования контуров маркеров
        markerStroke = new BasicStroke(1.3f, BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_MITER, 10.0f, null, 0.0f);
        // Шрифт для подписей осей координат
        axisFont = new Font("Serif", Font.BOLD, 36);
        numsFont = new Font("Serif", Font.BOLD,  15);

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if(graphicsData != null) handleMouseMoved(e);
            }
            @Override
            public void mouseDragged(MouseEvent e) {
                handleMouseMoved(e);
                if (selecting) {
                    selectionEnd = new Point2D.Double(e.getPoint().getX(), e.getPoint().getY());
                    repaint();
                }
            }
        });
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    handleMousePressed(e);
                    if(!isTracked)
                    {
                        selectionStart = new Point2D.Double(e.getPoint().getX(), e.getPoint().getY());
                        selectionEnd = null;
                        selecting = true;
                    }
                } else if (e.getButton() == MouseEvent.BUTTON3) {
                    resetZoom();
                }

            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (selecting && selectionStart != null && selectionEnd != null) {
                    zoomToSelection();
                }
                if(e.getButton() == MouseEvent.BUTTON1)
                {
                    handleMouseReleased(e);
                }
                selecting = false;
                selectionStart = null;
                selectionEnd = null;
                repaint();
            }
        });
    }

    // Данный метод вызывается из обработчика элемента меню "Открыть файл с графиком"
    // главного окна приложения в случае успешной загрузки данных
    public void showGraphics(Double[][] graphicsData) {
        // Сохранить массив точек во внутреннем поле класса

        graphicsData90 = new Double[graphicsData.length][];
        for(int i = 0; i < graphicsData.length; i++)
        {
            graphicsData90[i] = new Double[] {-1 * graphicsData[i][1], graphicsData[i][0]};
        }
        graphicsData1 = graphicsData;
        this.graphicsData = graphicsData;

        minX = graphicsData[0][0];
        maxX = graphicsData[0][0];
        minY = graphicsData[0][1];
        maxY = graphicsData[0][1];

        // Найти минимальное и максимальное значение функции
        for (int i = 1; i < graphicsData.length; i++) {
            if (graphicsData[i][1] < minY) {
                minY = graphicsData[i][1];
            }
            if (graphicsData[i][1] > maxY) {
                maxY = graphicsData[i][1];
            }
            if (graphicsData[i][0] < minX) {
                minX = graphicsData[i][0];
            }
            if (graphicsData[i][0] > maxX) {
                maxX = graphicsData[i][0];
            }
        }
        // Запросить перерисовку компонента, т.е. неявно вызвать paintComponent()
        repaint();
    }

    // Методы-модификаторы для изменения параметров отображения графика
    // Изменение любого параметра приводит к перерисовке области
    public void setShowAxis(boolean showAxis) {
        this.showAxis = showAxis;
        repaint();
    }

    public void setTurn90(boolean turn90) {
        this.turn90 = turn90;
        repaint();
    }

    public void setShowMarkers(boolean showMarkers) {
        this.showMarkers = showMarkers;
        repaint();
    }

    // Метод отображения всего компонента, содержащего график
    public void paintComponent(Graphics g) {
        /* Шаг 1 - Вызвать метод предка для заливки области цветом заднего фона
         * Эта функциональность - единственное, что осталось в наследство от
         * paintComponent класса JPanel
         */
        super.paintComponent(g);
        // Шаг 2 - Если данные графика не загружены (при показе компонента при запуске программы) - ничего не делать
        if (graphicsData == null || graphicsData.length == 0) return;
        // Шаг 3 - Определить минимальное и максимальное значения для координат X и
        // Это необходимо для определения области пространства, подлежащей отображению
        // Еѐ верхний левый угол это (minX, maxY) - правый нижний это (maxX, minY)
        width = getWidth();
        height = getHeight();

        if(turn90) this.graphicsData = graphicsData90;
        else  this.graphicsData = graphicsData1;

  /* Шаг 4 - Определить (исходя из размеров окна) масштабы по осям X
и Y - сколько пикселов
   * приходится на единицу длины по X и по Y
   */
        scaleX = ((double) width - 16) / (maxX - minX);
        scaleY = ((double) height - 16) / (maxY - minY);

        Graphics2D canvas = (Graphics2D) g;
        Stroke oldStroke = canvas.getStroke();
        Color oldColor = canvas.getColor();
        Paint oldPaint = canvas.getPaint();
        Font oldFont = canvas.getFont();
        // Шаг 8 - В нужном порядке вызвать методы отображения элементов графика
        // Порядок вызова методов имеет значение, т.к. предыдущий рисунок будет затираться последующим
        // Первыми (если нужно) отрисовываются оси координат.
        drawGrid(canvas);
        if (showAxis) paintAxis(canvas);
        // Затем отображается сам график
        paintGraphics(canvas);
        // Затем (если нужно) отображаются маркеры точек, по которым строился график.
        if (showMarkers) paintMarkers(canvas);

        if (highlightedPoint != null && highlightedCoords != null && !isTracked) {
            canvas.setColor(Color.RED);
            canvas.fillOval((int) highlightedPoint.x - 5, (int) highlightedPoint.y - 5, 10, 10);
            canvas.setColor(Color.BLACK);
            canvas.setFont(numsFont);
            canvas.drawString(highlightedCoords, (int) highlightedPoint.x + 10, (int) highlightedPoint.y - 10);
        }

        if (selectionStart != null && selectionEnd != null) {
            canvas.setColor(Color.DARK_GRAY);
            canvas.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{5}, 0));
            int x = (int) Math.min(selectionStart.x, selectionEnd.x);
            int y = (int)Math.min(selectionStart.y, selectionEnd.y);
            int widthRect = (int)Math.abs(selectionStart.x - selectionEnd.x);
            int heightRect = (int)Math.abs(selectionStart.y - selectionEnd.y);
            canvas.drawRect(x, y, widthRect, heightRect);
        }

        // Шаг 9 - Восстановить старые настройки холста
        canvas.setFont(oldFont);
        canvas.setPaint(oldPaint);
        canvas.setColor(oldColor);
        canvas.setStroke(oldStroke);
    }

    // Отрисовка графика по прочитанным координатам
    protected void paintGraphics(Graphics2D canvas) {
        // Выбрать линию для рисования графика
        canvas.setStroke(graphicsStroke);
        // Выбрать цвет линии
        canvas.setColor(Color.RED);

  /* Будем рисовать линию графика как путь, состоящий из множества
сегментов (GeneralPath)
   * Начало пути устанавливается в первую точку графика, после чего
прямой соединяется со
   * следующими точками
   */

        GeneralPath graphics = new GeneralPath();
        for (int i = 0; i < graphicsData.length; i++) {
            // Преобразовать значения (x,y) в точку на экране point
            Point2D.Double point = xyToPoint(graphicsData[i][0],
                    graphicsData[i][1]);


            if (i > 0) {
                // Не первая итерация цикла - вести линию в точку point
                graphics.lineTo(point.getX(), point.getY());
            } else {
                // Первая итерация цикла - установить начало пути в точку point
                graphics.moveTo(point.getX(), point.getY());
            }
        }
        // Отобразить график
        canvas.draw(graphics);
    }

    private boolean hasOnlyEvenDigits(double number) {
        long integerPart = (long) number;
        String integerPartStr = Long.toString(Math.abs(integerPart));
        for (char digit : integerPartStr.toCharArray()) {
            if ((digit - '0') % 2 != 0) {
                return false;
            }
        }

        return true; // Все цифры четные
    }

    // Отображение маркеров точек, по которым рисовался график
    protected void paintMarkers(Graphics2D canvas) {
        // Шаг 1 - Установить специальное перо для черчения контуров маркеров
        canvas.setStroke(markerStroke);
        // Выбрать красный цвета для контуров маркеров
        canvas.setColor(Color.BLACK);
        // Выбрать красный цвет для закрашивания маркеров внутри
        canvas.setPaint(Color.BLACK);
        // Шаг 2 - Организовать цикл по всем точкам графика
        for (Double[] point : graphicsData) {
            if(hasOnlyEvenDigits(point[1])) canvas.setColor(Color.GREEN);
            else canvas.setColor(Color.BLACK);
            Point2D.Double center = xyToPoint(point[0], point[1]);
            // Угол прямоугольника - отстоит на расстоянии (3,3)
            Point2D.Double left = shiftPoint(center, -5, 0);
            Point2D.Double left1 = shiftPoint(left, 0, -2.5);
            Point2D.Double left2 = shiftPoint(left, 0, 2.5);
            Point2D.Double right = shiftPoint(center, 5, 0);
            Point2D.Double right1 = shiftPoint(right, 0, -2.5);
            Point2D.Double right2 = shiftPoint(right, 0, 2.5);
            Point2D.Double up = shiftPoint(center, 0, 5);
            Point2D.Double up1 = shiftPoint(up, 2.5, 0);
            Point2D.Double up2 = shiftPoint(up, -2.5, 0);
            Point2D.Double down = shiftPoint(center, 0, -5);
            Point2D.Double down1 = shiftPoint(down, -2.5, 0);
            Point2D.Double down2 = shiftPoint(down, 2.5, 0);
            canvas.draw(new Line2D.Double(left, right));
            canvas.draw(new Line2D.Double(left1, left2));
            canvas.draw(new Line2D.Double(right1, right2));
            canvas.draw(new Line2D.Double(up, down));
            canvas.draw(new Line2D.Double(up1, up2));
            canvas.draw(new Line2D.Double(down1, down2));
            // Задать эллипс по центру и диагонали
            //marker.setFrameFromCenter(center, corner);
            //canvas.draw(marker); // Начертить контур маркера
            //canvas.fill(marker); // Залить внутреннюю область маркера
        }
    }

    // Метод, обеспечивающий отображение осей координат
    protected void paintAxis(Graphics2D canvas) {
        // Установить особое начертание для осей
        canvas.setStroke(axisStroke);
        // Оси рисуются чѐрным цветом
        canvas.setColor(Color.BLACK);
        // Стрелки заливаются чѐрным цветом
        canvas.setPaint(Color.BLACK);
        // Подписи к координатным осям делаются специальным шрифтом
        canvas.setFont(axisFont);
        // Создать объект контекста отображения текста - для получения характеристик устройства (экрана)
        FontRenderContext context = canvas.getFontRenderContext();
        // Определить, должна ли быть видна ось Y на графике
        if (minX <= 0.0 && maxX >= 0.0) {
            // Она должна быть видна, если левая граница показываемой области (minX) <= 0.0,
            // а правая (maxX) >= 0.0
            // Сама ось - это линия между точками (0, maxY) и (0, minY)
            canvas.draw(new Line2D.Double(xyToPoint(0, maxY), xyToPoint(0, minY)));
            // Стрелка оси Y
            GeneralPath arrow = new GeneralPath();
            // Установить начальную точку ломаной точно на верхний конец оси Y
            Point2D.Double lineEnd = xyToPoint(0, maxY);
            arrow.moveTo(lineEnd.getX(), lineEnd.getY());
            // Вести левый "скат" стрелки в точку с относительными координатами (5,20)
            arrow.lineTo(arrow.getCurrentPoint().getX() + 5,
                    arrow.getCurrentPoint().getY() + 20);
            // Вести нижнюю часть стрелки в точку с относительными координатами (-10, 0)
            arrow.lineTo(arrow.getCurrentPoint().getX() - 10,
                    arrow.getCurrentPoint().getY());
            // Замкнуть треугольник стрелки
            arrow.closePath();
            canvas.draw(arrow); // Нарисовать стрелку
            canvas.fill(arrow); // Закрасить стрелку
            // Нарисовать подпись к оси Y
            // Определить, сколько места понадобится для надписи "y"
            Rectangle2D bounds;
            if(turn90)  bounds = axisFont.getStringBounds("x", context);
            else  bounds = axisFont.getStringBounds("y", context);
            Point2D.Double labelPos = xyToPoint(0, maxY);
            // Вывести надпись в точке с вычисленными координатами
            if(!turn90) canvas.drawString("y", (float) labelPos.getX() + 10,
                    (float) (labelPos.getY() - bounds.getY()));
            else canvas.drawString("x", (float) labelPos.getX() - 27,
                    (float) (labelPos.getY() - bounds.getY()));
        }
        // Определить, должна ли быть видна ось X на графике
        if (minY <= 0.0 && maxY >= 0.0) {
            // Она должна быть видна, если верхняя граница показываемой области (maxX) >= 0.0,
            // а нижняя (minY) <= 0.0
            canvas.draw(new Line2D.Double(xyToPoint(minX, 0),
                    xyToPoint(maxX, 0)));
            // Стрелка оси X
            GeneralPath arrow = new GeneralPath();
            Point2D.Double lineEnd;
            if(!turn90)
            {
                // Установить начальную точку ломаной точно на правый конец оси X
                lineEnd = xyToPoint(maxX, 0);
                arrow.moveTo(lineEnd.getX(), lineEnd.getY());
                // Вести верхний "скат" стрелки в точку с относительными координатами (-20,-5)
                arrow.lineTo(arrow.getCurrentPoint().getX() - 20,
                        arrow.getCurrentPoint().getY() - 5);
                // Вести левую часть стрелки в точку с относительными координатами (0, 10)
                arrow.lineTo(arrow.getCurrentPoint().getX(),
                        arrow.getCurrentPoint().getY() + 10);
            }
            else
            {
                lineEnd = xyToPoint(minX, 0);
                arrow.moveTo(lineEnd.getX(), lineEnd.getY());
                // Вести верхний "скат" стрелки в точку с относительными координатами (-20,-5)
                arrow.lineTo(arrow.getCurrentPoint().getX() + 20,
                        arrow.getCurrentPoint().getY() - 5);
                // Вести левую часть стрелки в точку с относительными координатами (0, 10)
                arrow.lineTo(arrow.getCurrentPoint().getX(),
                        arrow.getCurrentPoint().getY() + 10);
            }
            // Замкнуть треугольник стрелки
            arrow.closePath();
            canvas.draw(arrow); // Нарисовать стрелку
            canvas.fill(arrow); // Закрасить стрелку
            // Нарисовать подпись к оси X
            // Определить, сколько места понадобится для надписи "x"
            Rectangle2D bounds;
            if(turn90) bounds = axisFont.getStringBounds("y", context);
            else bounds = axisFont.getStringBounds("x", context);
            Point2D.Double labelPos;
            if(!turn90)  labelPos = xyToPoint(maxX, 0);
            else labelPos = xyToPoint(minX, 0);
            // Вывести надпись в точке с вычисленными координатами

            if(!turn90) canvas.drawString("x", (float) (labelPos.getX() -
                    bounds.getWidth() - 10), (float) (labelPos.getY() + bounds.getY()));
            else canvas.drawString("y", (float) (labelPos.getX() -
                    bounds.getWidth() + 10), (float) (labelPos.getY() + bounds.getY()));

        }
    }

    protected Point2D.Double xyToPoint(double x, double y) {
        double x1 = 8.0 + ((x - minX) * scaleX);
        double y1 = (double)height - 8.0 - ((y -  minY) * scaleY);
        return new Point2D.Double(x1, y1);
    }

    protected Point2D.Double pointToxy(int x, int y) {
        double x1 = ((x - 8.0) / scaleX) + minX;
        double y1 = ((y - (double) height + 8.0) / scaleY) * -1 + minY;
        return new Point2D.Double(x1, y1);
    }

    protected double xToPoint(double x) {
        double x1 = 8.0 + ((x - minX) * scaleX);
        return  x1;
    }
    protected double yToPoint(double y) {
        double y1 = (double)height - 8.0 - ((y -  minY) * scaleY);
        return y1;
    }

    private void drawGrid(Graphics2D canvas) {
        canvas.setStroke(markerStroke);
        canvas.setColor(Color.LIGHT_GRAY);

        double xStep = Math.abs(xToPoint(maxX) - xToPoint(minX)) / 10;
        double yStep = Math.abs(yToPoint(maxY) - yToPoint(minY)) / 10;
        double xst = (maxX - minX) / 10;
        double x0 = minX;
        double yst = (maxY - minY) / 10;
        double y0 = maxY;
        int orderx = (int) Math.abs(Math.floor(Math.log10(Math.abs(minX)))) + 1;
        int ordery = (int) Math.abs(Math.floor(Math.log10(Math.abs(minY)))) + 1;
        String strx = "%." + Integer.toString(orderx) + "f";
        String stry = "%." + Integer.toString(ordery) + "f";
        for (double x = xToPoint(minX); x <= xToPoint(maxX); x += xStep) {
            canvas.draw(new Line2D.Double(x,yToPoint(minY), x, yToPoint(maxY)));
            canvas.drawString(String.format(strx, x0), (int) x + 1, (int)yToPoint(0) - 10);
            x0+= xst;
            double t = 0;
            for(double xx = x; xx < x + xStep; xx += xStep / 10)
            {
                Point2D.Double p1 = new Point2D.Double(xx, yToPoint(0));
                Point2D.Double p2 = shiftPoint(p1, 0, -10);
                if(t == 5) p2 = shiftPoint(p2, 0, -5);

                canvas.draw(new Line2D.Double(p1, p2));
                t++;
            }
        }

        for (double y = yToPoint(maxY); y <= yToPoint(minY); y += yStep) {
            canvas.draw(new Line2D.Double(xToPoint(minX),y, xToPoint(maxX), y));
            canvas.drawString(String.format(stry, y0), (int) xToPoint(0) + 10, (int) y + 1);
            y0-= yst;
            for(double yy = y; yy < y + yStep; yy += yStep / 10)
            {
                Point2D.Double p1 = new Point2D.Double(xToPoint(0), yy);
                Point2D.Double p2 = shiftPoint(p1, 10, 0);

                canvas.draw(new Line2D.Double(p1, p2));
            }
        }
        canvas.setColor(Color.BLACK);
    }

    protected Point2D.Double shiftPoint(Point2D.Double src, double deltaX, double deltaY) {
        // Инициализировать новый экземпляр точки
        Point2D.Double dest = new Point2D.Double();
        // Задать еѐ координаты как координаты существующей точки + заданные смещения
        dest.setLocation(src.getX() + deltaX, src.getY() + deltaY);
        return dest;
    }

    private void handleMouseMoved(MouseEvent e) {
        if (graphicsData.length == 0) {
            return;
        }

        int mouseX = e.getX();
        int mouseY = e.getY();
        highlightedPoint = null;
        highlightedCoords = null;
        int orderx = (int) Math.abs(Math.floor(Math.log10(Math.abs(minX)))) + 1;
        int ordery = (int) Math.abs(Math.floor(Math.log10(Math.abs(maxY)))) + 1;
        String strx = "%." + Integer.toString(orderx) + "f";
        String stry = "%." + Integer.toString(ordery) + "f";
        String str = "(" + strx + ", " + stry + ")";
        if(isTracked)
        {
            graphicsData[draggedPointIndex][0] =  pointToxy(mouseX, mouseY).getX();
            graphicsData[draggedPointIndex][1] = pointToxy(mouseX, mouseY).getY();
            repaint();
            return;
        }

        for (int i = 0; i < graphicsData.length; i++) {
            Point2D.Double p = xyToPoint(graphicsData[i][0], graphicsData[i][1]);
            if (Math.abs(mouseX - p.getX()) < 10 && Math.abs(mouseY - p.getY()) < 10) {
                highlightedPoint = p;
                highlightedCoords = String.format(str, graphicsData[i][0], graphicsData[i][1]);
                break;
            }
        }

        repaint();
    }

    private void handleMousePressed(MouseEvent e) {
        if (graphicsData == null || graphicsData.length == 0) {
            return;
        }
        int mouseX = e.getX();
        int mouseY = e.getY();

        for (int i = 0; i < graphicsData.length; i++) {
            Point2D.Double p = xyToPoint(graphicsData[i][0], graphicsData[i][1]);
            if (Math.abs(mouseX - p.getX()) < 10 && Math.abs(mouseY - p.getY()) < 10) {
                draggedPointIndex = i;
                isTracked = true;
                break;
            }
        }

        repaint();
    }

    private void handleMouseReleased(MouseEvent e) {
        if (graphicsData.length == 0) {
            return;
        }
        int mouseX = e.getX();
        int mouseY = e.getY();

        if(isTracked)
        {
            graphicsData[draggedPointIndex][0] = pointToxy(mouseX, mouseY).getX();
            graphicsData[draggedPointIndex][1] = pointToxy(mouseX, mouseY).getY();
            isTracked = false;
            highlightedPoint = pointToxy(mouseX, mouseY);
        }
        repaint();
    }

    private void zoomToSelection() {
        if (selectionStart == null || selectionEnd == null) {
            return;
        }

        int x1 = (int) Math.min(selectionStart.x, selectionEnd.x);
        int y1 = (int) Math.min(selectionStart.y, selectionEnd.y);
        int x2 = (int) Math.max(selectionStart.x, selectionEnd.x);
        int y2 = (int) Math.max(selectionStart.y, selectionEnd.y);

        double newMinX = minX + (x1 - 8) / scaleX;
        double newMaxX = minX + (x2 - 8) / scaleX;
        double newMinY = minY + (height - 8 - y2) / scaleY;
        double newMaxY = minY + (height - 8 - y1) / scaleY;

        Double[] temp = new Double[4];
        temp[0] = minX; temp[1] = maxX;
        temp[2] = minY; temp[3] = maxY;
        memoryStack.add(temp);

        minX = newMinX;
        maxX = newMaxX;
        minY = newMinY;
        maxY = newMaxY;


        repaint();
    }

    private void resetZoom() {
        if (memoryStack.isEmpty()) {
            return;
        }
        Double[] temp = memoryStack.peek();
        minX = temp[0];
        maxX = temp[1];
        minY = temp[2];
        maxY = temp[3];
        memoryStack.pop();
        repaint();
    }

}